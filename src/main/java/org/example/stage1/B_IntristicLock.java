package org.example.stage1;

import org.example.toolbox.Lock;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Про тонкости интритстик локов в JVM HotSpot link: https://youtu.be/q2wtSR3kD_I
 */
@SuppressWarnings("all")
public class B_IntristicLock {

    public static void main(String[] args) {

        final Lock mutex = null; // lock implementation

        Runnable command = () -> {

            mutex.lock();

            try {
                // critical section
                Blackhole.consumeCPU(8);
            } finally {
                mutex.unlock();
            }
        };
    }

}
