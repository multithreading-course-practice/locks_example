package org.example.stage5.queue;

import org.example.toolbox.Lock;

import java.util.concurrent.atomic.AtomicInteger;

public class ArrayQueueLock implements Lock {

    final ThreadLocal<Integer> threadSlot
            = ThreadLocal.withInitial(() -> 0);

    final AtomicInteger tail
            = new AtomicInteger(0);

    final int capacity;
    volatile boolean[] flags;

    /**
     * @param capacity максимальное кол-во потоков, на которых может работать данный Lock
     */
    public ArrayQueueLock(int capacity) {
        this.capacity = capacity;
        this.flags = new boolean[capacity];
        this.flags[0] = true;
    }

    @Override
    public void lock() {
        // вычисляем слот для текущего потока - остаток от деления на размер очереди
        int slot = tail.getAndIncrement() % capacity; // SA
        // присваиваем текущему потоку вычисленный слот
        threadSlot.set(slot);
        // spin до тех пор, пока предыдущий поток с таким номером слота не исполнит задачу, иначе захватываем лок
        while (!flags[slot]) {}
    }

    @Override
    public void unlock() {
        int slot = threadSlot.get();
        flags[slot] = false;
        flags[(slot + 1) % capacity] = true;
    }
}
