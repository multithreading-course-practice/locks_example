package org.example.stage1;

import org.example.toolbox.Lock;
import org.example.stage5.queue.ArrayQueueLock;
import lombok.extern.log4j.Log4j2;
import org.example.toolbox.ThreadAPI;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class C_LockExample {

    public static void main(String[] args) {
        final int capacity = 10;

        final Lock lock = new ArrayQueueLock(capacity);

        final int[] counter = new int[1];

        final Callable<?> command = () -> {
            lock.lock();
            log.info("Поток {} захватил lock", ThreadAPI.threadID());
            Blackhole.consumeCPU(8); // какая-то работа на процессоре
            counter[0]++;
            log.info("Поток {} скоро отпустит lock", ThreadAPI.threadID());
            lock.unlock();
            return null;
        };

        try (final ExecutorService executorService = Executors.newFixedThreadPool(capacity)) {
            for (int i = 0; i < 3 * capacity; i++) {
                executorService.submit(command);
            }
        }

        log.info("counter = {}", counter);
    }
}
