package org.example.stage6.composite;

import org.example.toolbox.SmartLock;
import org.example.stage4.backoff.Backoff;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

public class CompositeLock implements SmartLock {

    private final static int SIZE = 16;

    private final static int MIN_BACKOFF = 32;
    private final static int MAX_BACKOFF = 512;

    private final AtomicStampedReference<LockNode> tail
            = new AtomicStampedReference<>(null, 0);

    private final LockNode[] waiting
            = new LockNode[SIZE];

    private final ThreadLocal<LockNode> threadNode
            = ThreadLocal.withInitial(() -> null);

    public CompositeLock() {
        for (int i = 0; i < SIZE; i++) {
            waiting[i] = new LockNode();
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        long startTime = System.nanoTime();
        // время, сколько мы готовы ждать
        long patience = TimeUnit.NANOSECONDS.convert(time, unit);
        // прогрессивная задержка между попытками
        final Backoff backoff = new Backoff(MIN_BACKOFF, MAX_BACKOFF);

        try {
            // пытаемся найти свободную ноду
            LockNode acquire = acquireLock(startTime, patience, backoff);
            // добавляем ноду в хвост и возвращаем предшественника
            LockNode predecessor = spliceLock(acquire, startTime, patience);
            // ждем пока предшественник освободит лок
            waitForPredecessor(startTime, patience, predecessor, acquire);
            return true;
        } catch (TimeoutException timeoutException) {
            return false;
        }
    }

    private LockNode acquireLock(long startTime, long patience, Backoff backoff) throws TimeoutException {
        LockNode acquire = waiting[ThreadLocalRandom.current().nextInt(SIZE)];

        LockNode tailNode;
        int[] stamp = {0};

        for (; ; ) {
            // Ожидающий нод ограниченное количество равное SIZE
            // Поэтому потоки будут конкурировать за их получение

            // если нода свободна и ее текущее состояние FREE - то занимаем ее и выходим из метода
            if (acquire.state.compareAndSet(LockState.FREE, LockState.WAITING)) {
                return acquire;
            }

            // получаем ПАРУ текущий маркер (версия) записывается в stamp, а возвращаемый результат в tailNode
            tailNode = tail.get(stamp);
            LockState tailNodeState = tailNode.state.get();


            // если состояние нашего лока ABORTED или RELEASED
            // RELEASED - ждем пока поток захвативший нашу waiting ноду раньше освободит ее
            // ABORTED - не дождались пока предшественник освободит лок
            if (tailNodeState == LockState.ABORTED || tailNodeState == LockState.RELEASED) {

                // если в хвосте лежит инстанс нашей waiting ноды с состоянием ABORTED или RELEASED
                if (acquire == tailNode) {
                    LockNode predecessor = null;

                    // Если наша нода в хвосте и была отменена, это значит что фактически ее надо удалить из хвоста и вставить
                    // вместо нее предшественника
                    if (tailNodeState == LockState.ABORTED) {
                        predecessor = acquire.predecessor;
                    }

                    // пытаемся в хвост вставить предшественника, а так как нода была или отменена или зарелижена,
                    // то мы можем смело брать ее и использовать
                    if (tail.compareAndSet(tailNode, predecessor, stamp[0], stamp[0] + 1)) {
                        acquire.state.set(LockState.WAITING);
                        return acquire;
                    }
                }
            }

            backoff.delay();

            if (timeout(startTime, patience))
                throw new TimeoutException();
        }
    }

    private LockNode spliceLock(LockNode acquire, long startTime, long patience) throws TimeoutException {
        LockNode tailNode;
        int[] stamp = {0};

        do {
            // получаем ПАРУ текущий маркер (версия) записывается в stamp, а возвращаемый результат в tailNode
            tailNode = tail.get(stamp);

            if (timeout(startTime, patience)) {
                acquire.state.set(LockState.FREE);
                throw new TimeoutException();
            }

            // пытаемся в хвост запихнуть acquire ноду
        } while (!tail.compareAndSet(tailNode, acquire, stamp[0], stamp[0] + 1));

        return tailNode;
    }

    private void waitForPredecessor(long startTime, long patience, LockNode predecessor, LockNode acquire) throws TimeoutException {

        // если предшественника нет, то захватываем лок и выходим
        if(predecessor == null) {
            threadNode.set(acquire);
            return;
        }

        LockState predecessorState = predecessor.state.get();

        // пока предшественник не выполнит свое действие
        while (predecessorState != LockState.RELEASED) {

            // если предшественника отменили, то нам нужно убедиться что его предшественник освободил блокировку = то есть
            // нам надо дойти до такого состояния в котором написано что лок освобожден!
            if (predecessorState == LockState.ABORTED) {
                LockNode temporal = predecessor;
                predecessor = predecessor.predecessor;
                temporal.state.set(LockState.FREE);
            }

            // если пройдет отведенное время на захват блокировки и нам не удасться дождаться лока, то acquire считается отмененным
            if (timeout(startTime, patience)) {
                acquire.predecessor = predecessor;
                acquire.state.set(LockState.ABORTED);
                throw new TimeoutException();
            }

            // перечитываем в надежде, что что-то поменялось
            predecessorState = predecessor.state.get();
        }

        // если нам получилось захватить лок то мы помогаем предшественнику и ставим ему, то что он готов принимать следующие потоки
        predecessor.state.set(LockState.FREE);
        threadNode.set(acquire);
    }

    private boolean timeout(long startTime, long patience) {
        return System.nanoTime() - startTime >= patience;
    }

    @Override
    public void unlock() {
        LockNode acquire = threadNode.get();
        acquire.state.set(LockState.RELEASED);
        threadNode.set(null);
    }

    private enum LockState {
        FREE, WAITING, RELEASED, ABORTED
    }

    private static class LockNode {
        private final AtomicReference<LockState> state
                = new AtomicReference<>(LockState.FREE);

        private LockNode predecessor;
    }
}
