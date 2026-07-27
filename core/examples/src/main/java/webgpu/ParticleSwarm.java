package webgpu;

import processing.core.PApplet;
import processing.webgpu.PParticles;

/**
 * The simplest particle system: 40k particles, scattered into a ball, pushed
 * around by a curl-noise flow field. Nothing but position — no schema, no
 * attributes, no shader code. Each line in draw() does one thing to the whole
 * field.
 */
public class ParticleSwarm extends PApplet {

    PParticles p;

    public void settings() {
        size(900, 700, WEBGPU);
    }

    public void setup() {
        perspective(PI / 3, (float) width / height, 0.1f, 4000);
        camera(0, 0, 900, 0, 0, 0, 0, 1, 0);

        p = createParticles(40000);   // position only
        p.scatter(300);               // random positions → the whole field is alive
    }

    public void draw() {
        background(8, 8, 14);

        p.curlNoise(0.004f, 3.0f);    // curl-noise flow field (time auto-advances)

        particles(p);                 // default sphere sprite + unlit material
    }

    public static void main(String[] args) {
        PApplet.disableAWT = true;
        PApplet.main(ParticleSwarm.class.getName());
    }
}
