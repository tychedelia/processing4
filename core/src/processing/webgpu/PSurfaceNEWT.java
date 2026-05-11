package processing.webgpu;

import com.jogamp.nativewindow.Capabilities;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.nativewindow.WindowClosingProtocol;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PSurface;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bring-your-own-window PSurface that drives libprocessing through a JOGL
 * NEWT native window instead of LWJGL GLFW. The libprocessing FFI is
 * single-threaded (every input_* / *_draw call must happen on the thread
 * that called processing_init), but NEWT delivers events on its own EDT,
 * so listeners enqueue snapshots and the draw thread drains them.
 */
public class PSurfaceNEWT implements PSurface {

    protected PApplet sketch;
    protected PGraphics graphics;

    protected Display display;
    protected Screen screen;
    protected Window window;

    protected volatile boolean running = false;
    protected volatile boolean shouldClose = false;

    protected boolean paused;
    private final Lock pauseLock = new ReentrantLock();
    private final Condition pauseCondition = pauseLock.newCondition();

    protected float frameRateTarget = 60;
    protected long frameRatePeriod = 1000000000L / 60L;

    private static final AtomicInteger windowCount = new AtomicInteger(0);

    private volatile int currentMouseButton; // PConstants.LEFT/CENTER/RIGHT, or 0
    private volatile int currentModifiers;
    private volatile float lastCursorX;
    private volatile float lastCursorY;

    // NEWT delivers events on its EDT. The libprocessing FFI requires
    // every input_* / draw call to come from the thread that called
    // processing_init, so listeners snapshot the event into this queue
    // and the draw thread replays them through PWebGPU.
    private final ConcurrentLinkedQueue<PendingEvent> eventQueue =
            new ConcurrentLinkedQueue<>();

    // Coalesced — there's no point in replaying every intermediate resize
    // or move; we only need the latest at frame boundaries.
    private volatile int pendingResizeW = -1;
    private volatile int pendingResizeH = -1;
    private volatile int pendingMoveX;
    private volatile int pendingMoveY;
    private volatile boolean hasPendingMove;

    public PSurfaceNEWT(PGraphics graphics) {
        this.graphics = graphics;
    }

    @Override
    public void initOffscreen(PApplet sketch) {
        throw new IllegalStateException("PSurfaceNEWT does not support offscreen rendering");
    }

    @Override
    public void initFrame(PApplet sketch) {
        this.sketch = sketch;

        // Plain native window with no GL profile; we just want a handle to
        // hand to wgpu through libprocessing. Capabilities with no GL/EGL
        // hint avoids JOGL spinning up a context we'll never use.
        display = NewtFactory.createDisplay(null);
        display.addReference();
        screen = NewtFactory.createScreen(display, 0);
        screen.addReference();

        window = NewtFactory.createWindow(screen, new Capabilities());

        // Match PSurfaceGLFW: window opens non-resizable and we intercept
        // the close button so the sketch's exit path runs instead of NEWT
        // tearing down the window before we hear about it.
        window.setResizable(false);
        window.setDefaultCloseOperation(
                WindowClosingProtocol.WindowClosingMode.DO_NOTHING_ON_CLOSE);

        // On macOS Retina, NEWT defaults to reporting window sizes and
        // event coordinates in *pixels*, while GLFW (and therefore the
        // libprocessing scale-factor convention used by PSurfaceGLFW)
        // works in *points*. Enabling AUTOMAX_PIXELSCALE keeps mouse
        // coordinates in points and lets NEWT back the surface at native
        // pixel density — the same shape GLFW gives us out of the box.
        if (PApplet.platform == PConstants.MACOS) {
            window.setSurfaceScale(new float[] {
                    ScalableSurface.AUTOMAX_PIXELSCALE,
                    ScalableSurface.AUTOMAX_PIXELSCALE });
        }

        // PSurfaceJOGL's split: macOS keeps the window in points (AUTOMAX
        // backs the surface at native density), other platforms don't have
        // AUTOMAX so we multiply by pixelDensity to land a window whose
        // pixel size matches the sketch's logical size × density.
        int windowScaleFactor = (PApplet.platform == PConstants.MACOS)
                ? 1
                : Math.max(1, sketch.sketchPixelDensity());
        window.setSize(sketch.sketchWidth() * windowScaleFactor,
                sketch.sketchHeight() * windowScaleFactor);
        window.setTitle("Processing");

        // NEWT defers native window creation until the first setVisible(true);
        // we need a real handle below, so realize the window now (NEWT will
        // marshal to the platform's window-creation thread internally — on
        // macOS that's AppKit main, which is also where JVM main runs when
        // launched with -XstartOnFirstThread).
        window.setVisible(true);

        windowCount.incrementAndGet();

        initListeners();

        if (graphics instanceof PGraphicsWebGPU webgpu) {
            long windowHandle = getWindowHandle();
            long displayHandle = getDisplayHandle();
            int width = sketch.sketchWidth();
            int height = sketch.sketchHeight();
            float scaleFactor = sketch.sketchPixelDensity();

            // wgpu's Metal backend reads NSView.layer during surface +
            // graphics setup; Cocoa restricts that to the AppKit main
            // thread. With -XstartOnFirstThread, JVM-main is AppKit main
            // and this is a no-op. Without it, JOGL keeps AppKit on its
            // own thread, so we have to marshal these calls there.
            //
            // libprocessing also binds its App to whichever thread first
            // called processing_init via a thread_local, so by routing
            // init + surface_create + graphics_create through AppKit main
            // here we lock everything to a single thread that the draw
            // loop will then keep using each frame.
            runOnAppKitMain(() -> {
                PWebGPU.init();
                webgpu.initWebGPUSurface(windowHandle, displayHandle,
                        width, height, scaleFactor);
            });
        }
    }

    /**
     * Run {@code r} on the AppKit main thread on macOS, or inline on
     * other platforms / when already on main. Blocks until {@code r}
     * completes. Falls back to inline execution if jogamp's
     * {@code OSXUtil} isn't reachable for some reason — the caller then
     * sees the underlying "NSView on wrong thread" panic, which is more
     * useful than silently hanging.
     */
    private static void runOnAppKitMain(Runnable r) {
        if (PApplet.platform != PConstants.MACOS) {
            r.run();
            return;
        }
        try {
            if (jogamp.nativewindow.macosx.OSXUtil.IsMainThread()) {
                r.run();
            } else {
                jogamp.nativewindow.macosx.OSXUtil.RunOnMainThread(
                        /*waitUntilDone=*/true, /*kickNSApp=*/false, r);
            }
        } catch (NoClassDefFoundError | UnsatisfiedLinkError t) {
            r.run();
        }
    }

    protected void initListeners() {
        window.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseMoved(com.jogamp.newt.event.MouseEvent e) {
                queueMouseMove(e);
            }
            @Override
            public void mouseDragged(com.jogamp.newt.event.MouseEvent e) {
                queueMouseMove(e);
            }
            @Override
            public void mousePressed(com.jogamp.newt.event.MouseEvent e) {
                queueMouseButton(e, true);
            }
            @Override
            public void mouseReleased(com.jogamp.newt.event.MouseEvent e) {
                queueMouseButton(e, false);
            }
            @Override
            public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent e) {
                float[] rot = e.getRotation();
                int mods = e.getModifiers();
                currentModifiers = mods;
                float scale = pixelScale();
                int x = (int) (e.getX() / scale);
                int y = (int) (e.getY() / scale);
                eventQueue.add(PendingEvent.scroll(rot[0], rot[1], mods,
                        x, y, e.getWhen()));
            }
            @Override
            public void mouseEntered(com.jogamp.newt.event.MouseEvent e) {
                eventQueue.add(PendingEvent.cursorEnter(true));
            }
            @Override
            public void mouseExited(com.jogamp.newt.event.MouseEvent e) {
                eventQueue.add(PendingEvent.cursorEnter(false));
            }
        });

        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(com.jogamp.newt.event.KeyEvent e) {
                // NEWT fires synthetic auto-repeat events alongside the real
                // press/release pair; libprocessing's input_set_key already
                // handles repeat suppression on its own, so drop these.
                if (e.isAutoRepeat()) return;
                currentModifiers = e.getModifiers();
                eventQueue.add(PendingEvent.key(e, true));
            }
            @Override
            public void keyReleased(com.jogamp.newt.event.KeyEvent e) {
                if (e.isAutoRepeat()) return;
                currentModifiers = e.getModifiers();
                eventQueue.add(PendingEvent.key(e, false));
            }
        });

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(WindowEvent e) {
                // GLFW's framebuffer-size callback reports pixel dimensions;
                // match that by reading the surface (drawable) size, which
                // is at native pixel density when AUTOMAX is in effect.
                pendingResizeW = window.getSurfaceWidth();
                pendingResizeH = window.getSurfaceHeight();
            }
            @Override
            public void windowMoved(WindowEvent e) {
                pendingMoveX = window.getX();
                pendingMoveY = window.getY();
                hasPendingMove = true;
            }
            @Override
            public void windowGainedFocus(WindowEvent e) {
                eventQueue.add(PendingEvent.focus(true));
            }
            @Override
            public void windowLostFocus(WindowEvent e) {
                eventQueue.add(PendingEvent.focus(false));
            }
            @Override
            public void windowDestroyNotify(WindowEvent e) {
                // We asked NEWT to do nothing on close so the destroy hasn't
                // actually happened yet — flag the draw loop to call
                // sketch.exit(), which runs dispose() and tears the window
                // down through stopThread().
                shouldClose = true;
            }
        });
    }

    /**
     * Divisor that maps NEWT's native event coordinates back into the
     * logical units the sketch and libprocessing speak in.
     *
     * macOS: AUTOMAX makes the window itself live in points but NEWT
     * keeps reporting MouseEvent.getX/getY in pixels, so we divide by
     * the live surface scale (which can change if the user drags the
     * window onto a different display).
     *
     * Linux/Windows: no AUTOMAX, so event coords come back in whatever
     * unit setSize used — which is `sketch.width * pixelDensity` for the
     * non-mac branch of initFrame. Divide by pixelDensity to recover
     * logical units. (PSurfaceJOGL does the same split — see its
     * getCurrentPixelScale vs getPixelScale.)
     */
    private float pixelScale() {
        if (window == null) return 1f;
        if (PApplet.platform == PConstants.MACOS) {
            float[] s = new float[2];
            window.getCurrentSurfaceScale(s);
            return Math.max(1f, s[0]);
        }
        return sketch == null ? 1f : Math.max(1, sketch.sketchPixelDensity());
    }

    private void queueMouseMove(com.jogamp.newt.event.MouseEvent e) {
        float scale = pixelScale();
        float x = e.getX() / scale;
        float y = e.getY() / scale;
        lastCursorX = x;
        lastCursorY = y;
        int mods = e.getModifiers();
        currentModifiers = mods;
        eventQueue.add(PendingEvent.mouseMove(x, y, mods,
                currentMouseButton, e.getWhen()));
    }

    private void queueMouseButton(com.jogamp.newt.event.MouseEvent e, boolean pressed) {
        int peButton = switch (e.getButton()) {
            case com.jogamp.newt.event.MouseEvent.BUTTON1 -> PConstants.LEFT;
            case com.jogamp.newt.event.MouseEvent.BUTTON2 -> PConstants.CENTER;
            case com.jogamp.newt.event.MouseEvent.BUTTON3 -> PConstants.RIGHT;
            default -> 0;
        };
        int mods = e.getModifiers();
        currentModifiers = mods;
        if (pressed && peButton != 0) currentMouseButton = peButton;
        float scale = pixelScale();
        int x = (int) (e.getX() / scale);
        int y = (int) (e.getY() / scale);
        eventQueue.add(PendingEvent.mouseButton(peButton, pressed, mods,
                x, y, e.getWhen()));
        if (!pressed) currentMouseButton = 0;
    }

    /**
     * NEWT's window handle is the platform-native pointer with the same
     * shape libprocessing's surface_create expects per OS:
     *   macOS   NSWindow*  (processing_render translates NSWindow → NSView)
     *   Windows HWND
     *   X11     Window XID
     *   Wayland wl_surface*
     */
    public long getWindowHandle() {
        return window.getWindowHandle();
    }

    /**
     * For wgpu's X11/Wayland surface creation we also need the display
     * pointer. macOS and Windows can ignore it.
     */
    public long getDisplayHandle() {
        String type = NativeWindowFactory.getNativeWindowType(false);
        if (NativeWindowFactory.TYPE_MACOSX.equals(type)
                || NativeWindowFactory.TYPE_WINDOWS.equals(type)) {
            return 0;
        }
        return display.getHandle();
    }

    @Override
    public Object getNative() {
        return window;
    }

    @Override
    public void setTitle(String title) {
        if (window != null) {
            window.setTitle(title);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (window != null) {
            window.setVisible(visible);
        }
    }

    @Override
    public void setResizable(boolean resizable) {
        if (window != null) {
            // NEWT's term for resizable is "user-resizable"; setting this
            // flips the OS frame style on Windows/X11.
            window.setResizable(resizable);
        }
    }

    @Override
    public void setAlwaysOnTop(boolean always) {
        if (window != null) {
            window.setAlwaysOnTop(always);
        }
    }

    @Override
    public void setIcon(PImage icon) {
        // TODO: NEWT supports window icons via Display#setPointerIcon and
        // Window#setIcon(IconHandle), but neither maps trivially onto PImage
        // pixel data. Leave for a follow-up like PSurfaceGLFW does.
    }

    @Override
    public void placeWindow(int[] location, int[] editorLocation) {
        if (window == null) return;

        // NEWT.screen.getWidth/getHeight is in raw pixels, but
        // window.setPosition takes "window units" — which is points on
        // macOS (because AUTOMAX backs the surface at native density) and
        // pixels everywhere else. Likewise the window's own size in
        // window units is sketch.sketchWidth() on macOS but
        // sketch.sketchWidth() * pixelDensity on Linux/Windows. Without
        // these conversions, centering math on a HiDPI screen biases the
        // window toward the bottom-right by the density factor.
        int sw, sh;
        if (PApplet.platform == PConstants.MACOS) {
            float scale = pixelScale();
            sw = (int) (screen.getWidth()  / scale);
            sh = (int) (screen.getHeight() / scale);
        } else {
            sw = screen.getWidth();
            sh = screen.getHeight();
        }
        int factor = (PApplet.platform == PConstants.MACOS)
                ? 1
                : Math.max(1, sketch.sketchPixelDensity());
        int winW = sketch.sketchWidth()  * factor;
        int winH = sketch.sketchHeight() * factor;

        int x, y;
        if (location != null) {
            x = location[0];
            y = location[1];
        } else if (editorLocation != null) {
            x = editorLocation[0] - 20;
            y = editorLocation[1];

            if (x - winW < 10) {
                x = (sw - winW) / 2;
                y = (sh - winH) / 2;
            }
        } else {
            x = (sw - winW) / 2;
            y = (sh - winH) / 2;
        }

        window.setPosition(x, y);
    }

    @Override
    public void placePresent(int stopColor) {
        // TODO: present mode support
    }

    @Override
    public void setLocation(int x, int y) {
        if (window != null) {
            window.setPosition(x, y);
        }
    }

    @Override
    public void setSize(int width, int height) {
        if (width == sketch.width && height == sketch.height) {
            return;
        }
        sketch.width = width;
        sketch.height = height;
        graphics.setSize(width, height);
        if (window != null) {
            int factor = (PApplet.platform == PConstants.MACOS)
                    ? 1
                    : Math.max(1, sketch.sketchPixelDensity());
            window.setSize(width * factor, height * factor);
        }
    }

    @Override
    public void setFrameRate(float fps) {
        frameRateTarget = fps;
        frameRatePeriod = (long) (1000000000.0 / frameRateTarget);
    }

    @Override
    public void setCursor(int kind) {
        // TODO: cursor type mapping
    }

    @Override
    public void setCursor(PImage image, int hotspotX, int hotspotY) {
        // TODO: custom cursor
    }

    @Override
    public void showCursor() {
        if (window != null) {
            window.setPointerVisible(true);
        }
    }

    @Override
    public void hideCursor() {
        if (window != null) {
            window.setPointerVisible(false);
        }
    }

    @Override
    public PImage loadImage(String path, Object... args) {
        // TODO: AWT-free image loading
        throw new UnsupportedOperationException("Image loading not yet implemented for WebGPU");
    }

    @Override
    public boolean openLink(String url) {
        // TODO: AWT-free link opening
        return false;
    }

    @Override
    public void selectInput(String prompt, String callback, File file, Object callbackObject) {
        throw new UnsupportedOperationException("File dialogs not yet implemented for WebGPU");
    }

    @Override
    public void selectOutput(String prompt, String callback, File file, Object callbackObject) {
        throw new UnsupportedOperationException("File dialogs not yet implemented for WebGPU");
    }

    @Override
    public void selectFolder(String prompt, String callback, File file, Object callbackObject) {
        throw new UnsupportedOperationException("Folder selection not yet implemented for WebGPU");
    }

    @Override
    public void startThread() {
        if (running) {
            throw new IllegalStateException("Draw loop already running");
        }
        running = true;

        // On macOS, wgpu's Metal calls must happen on AppKit main; we
        // can't run the whole draw loop there because then AppKit's run
        // loop never gets to pump NSEvents and the window deadlocks
        // after the first frame. So the draw thread handles timing and
        // dispatches each frame's wgpu work onto AppKit main, returning
        // to the run loop in between. PSurfaceJOGL has always followed
        // this same async-startThread pattern; PApplet.runSketch
        // tolerates it (PSurfaceGLFW's blocking variant only works
        // because GLFW pumps NSEvents inside glfwPollEvents).
        Thread drawThread = new Thread(this::runDrawLoop, "PSurfaceNEWT-draw");
        drawThread.setDaemon(false);
        drawThread.start();
    }

    protected void runDrawLoop() {
        // NEWT's requestFocus already marshals to its windowing thread,
        // safe to call from here.
        window.requestFocus();

        long beforeTime = System.nanoTime();
        long overSleepTime = 0L;

        // sketch.start() runs the user's setup() if it hasn't run yet;
        // setup() can call PWebGPU.* directly, so it has to be on the
        // FFI's bound thread (AppKit main).
        runOnAppKitMain(sketch::start);

        while (running) {
            checkPause();

            if (shouldClose) {
                runOnAppKitMain(sketch::exit);
                break;
            }

            // Coalesce the latest resize/move once per frame. The replays
            // touch PWebGPU.windowResized via PGraphicsWebGPU.setSize, so
            // they have to run on AppKit main alongside the rest of the
            // per-frame work.
            int rw = pendingResizeW;
            int rh = pendingResizeH;
            if (rw > 0 && rh > 0) {
                pendingResizeW = pendingResizeH = -1;
            } else {
                rw = -1; rh = -1;
            }
            boolean doMove = hasPendingMove;
            int mx = pendingMoveX;
            int my = pendingMoveY;
            if (doMove) hasPendingMove = false;

            final int fRw = rw, fRh = rh;
            final boolean fDoMove = doMove;
            final int fMx = mx, fMy = my;

            // The entire frame body runs on AppKit main: replay events,
            // flush input, run user's draw(). Between dispatches main
            // returns to the AppKit run loop and drains NSEvents — that's
            // the bit that fixes the first-frame-only deadlock.
            runOnAppKitMain(() -> {
                if (fRw > 0 && fRh > 0 && sketch != null) {
                    sketch.postWindowResized(fRw, fRh);
                }
                if (fDoMove && sketch != null) {
                    sketch.postWindowMoved(fMx, fMy);
                }

                long surfaceId = getSurfaceId();
                PendingEvent ev;
                while ((ev = eventQueue.poll()) != null) {
                    ev.applyTo(surfaceId, sketch);
                }
                if (surfaceId != 0) {
                    PWebGPU.inputFlush();
                }
                if (!sketch.finished) {
                    sketch.handleDraw();
                }
            });

            long afterTime = System.nanoTime();
            long timeDiff = afterTime - beforeTime;
            long sleepTime = (frameRatePeriod - timeDiff) - overSleepTime;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime / 1000000L, (int) (sleepTime % 1000000L));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
            } else {
                overSleepTime = 0L;
            }
            beforeTime = System.nanoTime();
        }

        runOnAppKitMain(sketch::dispose);
    }

    private long getSurfaceId() {
        if (graphics instanceof PGraphicsWebGPU webgpu) {
            return webgpu.getSurfaceId();
        }
        return 0;
    }

    @Override
    public void pauseThread() {
        paused = true;
    }

    protected void checkPause() {
        if (paused) {
            pauseLock.lock();
            try {
                while (paused) {
                    pauseCondition.await();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                pauseLock.unlock();
            }
        }
    }

    @Override
    public void resumeThread() {
        pauseLock.lock();
        try {
            paused = false;
            pauseCondition.signalAll();
        } finally {
            pauseLock.unlock();
        }
    }

    @Override
    public boolean stopThread() {
        if (!running) {
            return false;
        }
        running = false;

        if (window != null) {
            window.destroy();
            window = null;
        }
        if (screen != null) {
            screen.removeReference();
            screen = null;
        }
        if (display != null) {
            display.removeReference();
            display = null;
        }

        windowCount.decrementAndGet();
        return true;
    }

    @Override
    public boolean isStopped() {
        return !running;
    }

    // ── Key code translation ──────────────────────────────────────────

    /**
     * Translate a NEWT keycode (AWT-shaped: e.g. VK_ESCAPE = 27, VK_F1 = 112)
     * into the GLFW-shaped code that libprocessing's processing_input_key
     * expects (see PROCESSING_KEY_* in processing_ffi). Returns 0 if NEWT
     * reports a key with no GLFW equivalent in libprocessing's table;
     * callers skip the FFI call in that case.
     *
     * Limitation: NEWT's KeyEvent in jogl 2.6 doesn't expose left/right
     * modifier location, so VK_SHIFT/VK_CONTROL/VK_ALT/VK_META always
     * map to their LEFT variant. A sketch that wants to distinguish
     * left from right control will not see the right one as distinct
     * here — same trade-off PSurfaceJOGL has lived with.
     */
    private static int newtKeyCodeToGlfw(short newtKey) {
        // Letters and digits already align with GLFW (uppercase ASCII).
        if ((newtKey >= 'A' && newtKey <= 'Z') || (newtKey >= '0' && newtKey <= '9')) {
            return newtKey;
        }

        // Numpad digits: NEWT VK_NUMPAD0..9 → PROCESSING_KEY_NUMPAD_0..9
        if (newtKey >= com.jogamp.newt.event.KeyEvent.VK_NUMPAD0
                && newtKey <= com.jogamp.newt.event.KeyEvent.VK_NUMPAD9) {
            return 320 + (newtKey - com.jogamp.newt.event.KeyEvent.VK_NUMPAD0);
        }

        return switch (newtKey) {
            case com.jogamp.newt.event.KeyEvent.VK_SPACE -> 32;
            case com.jogamp.newt.event.KeyEvent.VK_COMMA -> 44;
            case com.jogamp.newt.event.KeyEvent.VK_MINUS -> 45;
            case com.jogamp.newt.event.KeyEvent.VK_PERIOD -> 46;
            case com.jogamp.newt.event.KeyEvent.VK_SLASH -> 47;
            case com.jogamp.newt.event.KeyEvent.VK_SEMICOLON -> 59;
            case com.jogamp.newt.event.KeyEvent.VK_EQUALS -> 61;
            case com.jogamp.newt.event.KeyEvent.VK_OPEN_BRACKET -> 91;   // BRACKET_LEFT
            case com.jogamp.newt.event.KeyEvent.VK_BACK_SLASH -> 92;
            case com.jogamp.newt.event.KeyEvent.VK_CLOSE_BRACKET -> 93;  // BRACKET_RIGHT
            case com.jogamp.newt.event.KeyEvent.VK_BACK_QUOTE -> 96;
            case com.jogamp.newt.event.KeyEvent.VK_QUOTE -> 39;          // apostrophe
            case com.jogamp.newt.event.KeyEvent.VK_ESCAPE -> 256;
            case com.jogamp.newt.event.KeyEvent.VK_ENTER -> 257;
            case com.jogamp.newt.event.KeyEvent.VK_TAB -> 258;
            case com.jogamp.newt.event.KeyEvent.VK_BACK_SPACE -> 259;
            case com.jogamp.newt.event.KeyEvent.VK_INSERT -> 260;
            case com.jogamp.newt.event.KeyEvent.VK_DELETE -> 261;
            case com.jogamp.newt.event.KeyEvent.VK_RIGHT -> 262;
            case com.jogamp.newt.event.KeyEvent.VK_LEFT -> 263;
            case com.jogamp.newt.event.KeyEvent.VK_DOWN -> 264;
            case com.jogamp.newt.event.KeyEvent.VK_UP -> 265;
            case com.jogamp.newt.event.KeyEvent.VK_PAGE_UP -> 266;
            case com.jogamp.newt.event.KeyEvent.VK_PAGE_DOWN -> 267;
            case com.jogamp.newt.event.KeyEvent.VK_HOME -> 268;
            case com.jogamp.newt.event.KeyEvent.VK_END -> 269;
            case com.jogamp.newt.event.KeyEvent.VK_CAPS_LOCK -> 280;
            case com.jogamp.newt.event.KeyEvent.VK_SCROLL_LOCK -> 281;
            case com.jogamp.newt.event.KeyEvent.VK_NUM_LOCK -> 282;
            case com.jogamp.newt.event.KeyEvent.VK_PRINTSCREEN -> 283;
            case com.jogamp.newt.event.KeyEvent.VK_PAUSE -> 284;
            case com.jogamp.newt.event.KeyEvent.VK_F1 -> 290;
            case com.jogamp.newt.event.KeyEvent.VK_F2 -> 291;
            case com.jogamp.newt.event.KeyEvent.VK_F3 -> 292;
            case com.jogamp.newt.event.KeyEvent.VK_F4 -> 293;
            case com.jogamp.newt.event.KeyEvent.VK_F5 -> 294;
            case com.jogamp.newt.event.KeyEvent.VK_F6 -> 295;
            case com.jogamp.newt.event.KeyEvent.VK_F7 -> 296;
            case com.jogamp.newt.event.KeyEvent.VK_F8 -> 297;
            case com.jogamp.newt.event.KeyEvent.VK_F9 -> 298;
            case com.jogamp.newt.event.KeyEvent.VK_F10 -> 299;
            case com.jogamp.newt.event.KeyEvent.VK_F11 -> 300;
            case com.jogamp.newt.event.KeyEvent.VK_F12 -> 301;
            case com.jogamp.newt.event.KeyEvent.VK_DECIMAL -> 330;
            case com.jogamp.newt.event.KeyEvent.VK_DIVIDE -> 331;
            case com.jogamp.newt.event.KeyEvent.VK_MULTIPLY -> 332;
            case com.jogamp.newt.event.KeyEvent.VK_SUBTRACT -> 333;
            case com.jogamp.newt.event.KeyEvent.VK_ADD -> 334;
            case com.jogamp.newt.event.KeyEvent.VK_SHIFT -> 340;         // SHIFT_LEFT
            case com.jogamp.newt.event.KeyEvent.VK_CONTROL -> 341;       // CONTROL_LEFT
            case com.jogamp.newt.event.KeyEvent.VK_ALT -> 342;           // ALT_LEFT
            case com.jogamp.newt.event.KeyEvent.VK_META,
                 com.jogamp.newt.event.KeyEvent.VK_WINDOWS -> 343;       // SUPER_LEFT
            case com.jogamp.newt.event.KeyEvent.VK_CONTEXT_MENU -> 348;
            default -> 0;
        };
    }

    // ── Event snapshots ────────────────────────────────────────────────

    /** A NEWT event captured on its EDT and replayed on the draw thread. */
    private static final class PendingEvent {
        enum Kind { MOUSE_MOVE, MOUSE_BUTTON, SCROLL, KEY, CURSOR_ENTER, FOCUS }

        final Kind kind;
        final float fA;
        final float fB;
        final int iA; // mods / button / keyCode
        final int iB; // x / pressed-bit / button-from-mouse-state
        final int iC; // y / count
        final boolean bool;
        final char ch;
        final long time;

        private PendingEvent(Kind kind, float fA, float fB, int iA, int iB, int iC,
                             boolean bool, char ch, long time) {
            this.kind = kind;
            this.fA = fA;
            this.fB = fB;
            this.iA = iA;
            this.iB = iB;
            this.iC = iC;
            this.bool = bool;
            this.ch = ch;
            this.time = time;
        }

        static PendingEvent mouseMove(float x, float y, int mods, int button, long t) {
            return new PendingEvent(Kind.MOUSE_MOVE, x, y, mods, button, 0, false, '\0', t);
        }

        static PendingEvent mouseButton(int peButton, boolean pressed, int mods,
                                        int x, int y, long t) {
            return new PendingEvent(Kind.MOUSE_BUTTON, x, y, mods, peButton, 0, pressed, '\0', t);
        }

        static PendingEvent scroll(float dx, float dy, int mods, int x, int y, long t) {
            return new PendingEvent(Kind.SCROLL, dx, dy, mods, x, y, false, '\0', t);
        }

        static PendingEvent key(com.jogamp.newt.event.KeyEvent e, boolean pressed) {
            // libprocessing's processing_input_key expects GLFW-shaped key
            // codes (see PROCESSING_KEY_* in processing_ffi). Translate
            // here on the listener thread so the draw thread doesn't need
            // to know about NEWT-specific codes.
            int glfwCode = newtKeyCodeToGlfw(e.getKeyCode());
            return new PendingEvent(Kind.KEY, 0, 0,
                    e.getModifiers(), glfwCode, e.getKeyCode(),
                    pressed, e.getKeyChar(), e.getWhen());
        }

        static PendingEvent cursorEnter(boolean entered) {
            return new PendingEvent(Kind.CURSOR_ENTER, 0, 0, 0, 0, 0, entered, '\0', 0);
        }

        static PendingEvent focus(boolean focused) {
            return new PendingEvent(Kind.FOCUS, 0, 0, 0, 0, 0, focused, '\0', 0);
        }

        void applyTo(long surfaceId, PApplet sketch) {
            switch (kind) {
                case MOUSE_MOVE -> {
                    if (surfaceId != 0) {
                        PWebGPU.inputMouseMove(surfaceId, fA, fB);
                    }
                    if (sketch != null) {
                        int action = (iB == 0) ? MouseEvent.MOVE : MouseEvent.DRAG;
                        sketch.postEvent(new MouseEvent(null, time, action, iA,
                                (int) fA, (int) fB, iB, 0));
                    }
                }
                case MOUSE_BUTTON -> {
                    int peButton = iB;
                    boolean pressed = bool;
                    if (surfaceId != 0 && peButton != 0) {
                        byte btn = (byte) (peButton == PConstants.LEFT ? 0
                                : peButton == PConstants.CENTER ? 1 : 2);
                        PWebGPU.inputMouseButton(surfaceId, btn, pressed);
                    }
                    if (sketch != null && peButton != 0) {
                        int peAction = pressed ? MouseEvent.PRESS : MouseEvent.RELEASE;
                        sketch.postEvent(new MouseEvent(null, time, peAction, iA,
                                (int) fA, (int) fB, peButton, 1));
                    }
                }
                case SCROLL -> {
                    if (surfaceId != 0) {
                        PWebGPU.inputScroll(surfaceId, fA, fB);
                    }
                    if (sketch != null) {
                        // Processing wheel uses negative-up; NEWT's rotation
                        // is positive-up, matching GLFW — flip to align.
                        sketch.postEvent(new MouseEvent(null, time, MouseEvent.WHEEL, iA,
                                iB, iC, 0, (int) -fB));
                    }
                }
                case KEY -> {
                    int glfwCode = iB;
                    int newtCode = iC;
                    boolean pressed = bool;
                    if (surfaceId != 0 && glfwCode != 0) {
                        PWebGPU.inputKey(surfaceId, glfwCode, pressed);
                        // NEWT bundles the character into the same event;
                        // mirror PSurfaceGLFW's char-callback path so sketches
                        // that read `key` get the typed character.
                        if (pressed && ch != com.jogamp.newt.event.KeyEvent.NULL_CHAR) {
                            PWebGPU.inputChar(surfaceId, glfwCode, ch);
                        }
                    }
                    if (sketch != null) {
                        // The sketch-visible KeyEvent uses the NEWT keycode
                        // (which matches java.awt.event.KeyEvent VK_* values
                        // — the convention PSurfaceJOGL has always exposed).
                        int peAction = pressed ? KeyEvent.PRESS : KeyEvent.RELEASE;
                        sketch.postEvent(new KeyEvent(null, time, peAction, iA,
                                ch == com.jogamp.newt.event.KeyEvent.NULL_CHAR
                                        ? PConstants.CODED : ch,
                                newtCode));
                        if (pressed && ch != com.jogamp.newt.event.KeyEvent.NULL_CHAR) {
                            sketch.postEvent(new KeyEvent(null, time, KeyEvent.TYPE, iA,
                                    ch, 0));
                        }
                    }
                }
                case CURSOR_ENTER -> {
                    if (surfaceId != 0) {
                        if (bool) PWebGPU.inputCursorEnter(surfaceId);
                        else PWebGPU.inputCursorLeave(surfaceId);
                    }
                }
                case FOCUS -> {
                    if (surfaceId != 0) {
                        PWebGPU.inputFocus(surfaceId, bool);
                    }
                }
            }
        }
    }
}
