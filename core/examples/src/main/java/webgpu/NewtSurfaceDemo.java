package webgpu;

import processing.core.PApplet;

/**
 * Minimal sketch that exercises PSurfaceNEWT — the JOGL NEWT-backed
 * windowing path for the WebGPU renderer.
 *
 * Visual feedback for each event class:
 *   • mouse move/drag → circle follows the cursor
 *   • mouse press     → circle gets larger while held
 *   • mouse wheel     → background hue shifts
 *   • any printable key (a–z, digits) → background hue jumps
 *   • arrow keys      → nudge the circle (proves special-key code path)
 *   • esc             → exit (proves keyCode translation + close path)
 */
public class NewtSurfaceDemo extends PApplet {

    float circleX, circleY;
    int circleR = 40;
    int bgHue = 200;
    int nudgeX = 0, nudgeY = 0;

    public void settings() {
        size(640, 480, WEBGPU);
    }

    public void setup() {
        circleX = width / 2f;
        circleY = height / 2f;
        colorMode(HSB, 360, 100, 100);
        noStroke();
    }

    public void draw() {
        background(bgHue, 60, 30);

        float targetX = mouseX + nudgeX;
        float targetY = mouseY + nudgeY;
        circleX += (targetX - circleX) * 0.15f;
        circleY += (targetY - circleY) * 0.15f;

        float r = mousePressed ? circleR * 1.8f : circleR;
        fill((bgHue + 180) % 360, 80, 95);
        ellipse(circleX, circleY, r, r);
    }

    public void mouseWheel(processing.event.MouseEvent e) {
        bgHue = (bgHue + e.getCount() * 10 + 360) % 360;
    }

    public void keyPressed() {
        if (keyCode == ESC) {
            // PApplet maps esc → exit() by default; we set key=0 here only
            // if we wanted to suppress it. Letting it fall through is fine.
            return;
        }
        if (keyCode == UP)         nudgeY -= 20;
        else if (keyCode == DOWN)  nudgeY += 20;
        else if (keyCode == LEFT)  nudgeX -= 20;
        else if (keyCode == RIGHT) nudgeX += 20;
        else if (key >= 'a' && key <= 'z') {
            bgHue = (int) map(key, 'a', 'z', 0, 360);
        }
    }

    public static void main(String[] args) {
        // Pick the JOGL NEWT surface instead of the default LWJGL GLFW one.
        // Must be set before PApplet.main() so PGraphicsWebGPU#createSurface
        // sees it on the first call.
        System.setProperty("processing.webgpu.surface", "newt");

        // PSurfaceNEWT (like PSurfaceGLFW) doesn't go through AWT, so the
        // sketch can run with AWT disabled — important on macOS where
        // AWT would otherwise try to claim the AppKit main thread.
        PApplet.disableAWT = true;
        PApplet.main(NewtSurfaceDemo.class.getName());
    }
}
