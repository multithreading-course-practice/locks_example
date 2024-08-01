package org.example.stage4.backoff;

import org.example.toolbox.ThreadAPI;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Backoff {

    final int minDelay, maxDelay;
    int limit;

    public Backoff(int minDelay, int maxDelay) {
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;

        this.limit = minDelay;
    }

    public void delay() {
        final int delay = ThreadLocalRandom.current().nextInt(limit);
        this.limit = Math.min(maxDelay, 2 * limit); // постепенно увеличиваем время засыпания
        ThreadAPI.sleep(delay, TimeUnit.MILLISECONDS);
    }
}
