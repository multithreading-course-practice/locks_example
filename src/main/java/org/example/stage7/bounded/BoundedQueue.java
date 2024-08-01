package org.example.stage7.bounded;


import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedQueue<T> {

    private final Lock sync
            = new ReentrantLock(true);

    private final Condition queueNotFull
            = sync.newCondition();

    private final Condition queueNotEmpty
            = sync.newCondition();

    private final T[] items;
    private int tail, head, count;

    public BoundedQueue(int capacity) {
        //noinspection unchecked
        this.items = (T[]) new Object[capacity];
    }

    public boolean add(T item) {
        sync.lock();

        try {

            // если размер очереди достиг максимального значения, то ждем
            while (count == items.length)
                queueNotFull.await();

            items[tail] = item;
            tail = tail + 1;
            if (tail == items.length) tail = 0; // замкнем круг

            count = count + 1;
            // сигнализируем потоку, который ждет на условии "очередь не пуста"
            //  if(count == 1)
            queueNotEmpty.signal();

            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            sync.unlock();
        }
    }

    public T poll() throws InterruptedException {
        sync.lock();

        try {
            while (count == 0)
                queueNotEmpty.await();

            T item = items[head];
            head = head + 1;
            if(head == items.length) head = 0; // замкнем круг

            count = count - 1;
            // сигнализируем потоку, который ждет на условии "очередь не полная"
            queueNotFull.signal();

            return item;
        } finally {
            sync.unlock();
        }
    }
}
