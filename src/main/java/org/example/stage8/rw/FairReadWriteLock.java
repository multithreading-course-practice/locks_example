package org.example.stage8.rw;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

public class FairReadWriteLock implements ReadWriteLock {

    private final Lock sync
            = new ReentrantLock(true);

    private final Condition condition
            = sync.newCondition();

    private final Lock readLock = new ReadLock();
    private final Lock writeLock = new WriteLock();

    private int readAcquires, readReleases = 0;
    private boolean writer = false;

    @Override
    public Lock readLock() {
        return readLock;
    }

    @Override
    public Lock writeLock() {
        return writeLock;
    }

    private class ReadLock implements Lock {

        @Override
        public void lock() {
            sync.lock();

            try {
                while (writer)
                    condition.await();

                readAcquires = readAcquires + 1;
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            } finally {
                sync.unlock();
            }
        }

        @Override
        public void unlock() {
            sync.lock();

            try {
                readReleases = readReleases + 1;

                if (readAcquires == readReleases)
                    condition.signalAll();
            } finally {
                sync.unlock();
            }
        }

        // region лишнее

        @Override
        public void lockInterruptibly() throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tryLock() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        // endregion
    }

    private class WriteLock implements Lock {

        @Override
        public void lock() {
            sync.lock();

            try {
                while (writer)
                    condition.await();

                writer = true;

                while (readAcquires != readReleases)
                    condition.await();

            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            } finally {
                sync.unlock();
            }
        }

        @Override
        public void unlock() {
            sync.lock();

            try {
                writer = false;
                condition.signalAll();
            } finally {
                sync.unlock();
            }
        }

        // region лишнее

        @Override
        public void lockInterruptibly() throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tryLock() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        // endregion
    }
}
