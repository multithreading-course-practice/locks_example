package org.example.stage4.backoff;

import org.example.toolbox.Lock;

import java.util.concurrent.atomic.AtomicBoolean;

public class BackoffLock implements Lock {

    private static final int MIN_DELAY = 32; // depends on platform, cpu etc
    private static final int MAX_DELAY = 512; // depends on platform, cpu etc

    private final AtomicBoolean state = new AtomicBoolean();

    @Override
    public void lock() {
        final Backoff backoff = new Backoff(MIN_DELAY, MAX_DELAY);

        while (true) {
            while (state.get()) {} // spin
            if(!state.getAndSet(true)) return;
            else backoff.delay(); // unfair, starvation
        }
    }

    @Override
    public void unlock() {
        state.set(false);
    }
}
