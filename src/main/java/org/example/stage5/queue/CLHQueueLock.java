package org.example.stage5.queue;

import org.example.toolbox.Lock;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Проблема при Non-Uniform архитектуре - читаем чужой участок памяти, который может быть на другом узле (дорогое чтение).
 */
public class CLHQueueLock implements Lock {

    private final AtomicReference<LockNode> tail
            = new AtomicReference<>(new LockNode());

    private final ThreadLocal<LockNode> predecessorNode
            = ThreadLocal.withInitial(() -> null);

    private final ThreadLocal<LockNode> threadNode
            = ThreadLocal.withInitial(LockNode::new);

    @Override
    public void lock() {
        // получим ноду для нашего потока и установим в нее флаг блокировки
        LockNode thread = threadNode.get();
        thread.locked = true; // SA

        // получаем текущее значение хвоста и устанавливаем новое
        LockNode predecessor = tail.getAndSet(thread); // SA
        predecessorNode.set(predecessor);

        while (predecessor.locked) {}
    }

    @Override
    public void unlock() {
        // пропускаем следующего кто синхронизирован на predecessor в строчке 26
        LockNode thread = threadNode.get();
        thread.locked = false;

        // рубрика сломай себе мозг - мы пропустили ждущий поток (синхронизированный на нас как на predecessor в строчке 26) и теперь можем переиспользовать предыдущую свободную ноду
        threadNode.set(predecessorNode.get());
    }

    private static class LockNode {
        private volatile boolean locked = false;
    }
}
