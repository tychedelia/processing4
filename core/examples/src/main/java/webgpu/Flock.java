package webgpu;

import processing.core.PApplet;
import processing.webgpu.Particles;

/**
 * A GPU flock of 20k boids. Still no schema and no shader code: the field
 * starts with only a position, and applying the flock kernel is what brings a
 * `velocity` attribute into existence — the behavior you dispatch defines the
 * shape of the particle. A sphere bound keeps the flock on screen.
 */
public class Flock extends PApplet {

    Particles p;

    public void settings() {
        size(900, 700, WEBGPU);
    }

    public void setup() {
        perspective(PI / 3, (float) width / height, 0.1f, 4000);
        camera(0, 0, 800, 0, 0, 0, 0, 1, 0);

        p = createParticles(20000);   // position only
        p.scatter(250);               // whole flock alive, random start positions
    }

    public void draw() {
        background(10, 12, 20);

        p.flock(45, 20);    // neighbor distance, separation — grows + writes velocity
        p.bounds(280);      // keep the flock inside a sphere

        particles(p);

        if (frameCount % 60 == 0) {
            println("fps: " + nf(frameRate, 0, 1));
        }
    }

    public static void main(String[] args) {
        PApplet.disableAWT = true;
        PApplet.main(Flock.class.getName());
    }
}
