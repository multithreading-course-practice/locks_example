package org.example.stage3.tas;

import org.example.toolbox.Lock;

import java.util.concurrent.atomic.AtomicBoolean;

// N-потоков
public class TASLock implements Lock {

    private final AtomicBoolean state = new AtomicBoolean(false);

    @Override
    public void lock() {
        while (state.getAndSet(true)) {} // spin
    }

    @Override
    public void unlock() {
        state.set(false);
    }

}
