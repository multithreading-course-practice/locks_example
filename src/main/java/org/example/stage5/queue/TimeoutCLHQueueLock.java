package org.example.stage5.queue;

import org.example.toolbox.SmartLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


public class TimeoutCLHQueueLock implements SmartLock {

    private final static LockNode AVAILABLE = new LockNode();

    private final AtomicReference<LockNode> tail
            = new AtomicReference<>(null);

    private final ThreadLocal<LockNode> threadNode
            = ThreadLocal.withInitial(LockNode::new);

    @Override
    public boolean tryLock(long time, TimeUnit unit) {

        long startTime = System.nanoTime();
        // время, сколько мы готовы ждать
        long patience = TimeUnit.NANOSECONDS.convert(time, unit);

        LockNode thread = new LockNode();
        threadNode.set(thread);

        // переменная будет несколько раз переписываться, поэтому называется поколением
        // первое чтение - это наш предшественние: поток X записывает себя приемником в Поток X-1
        LockNode generation = tail.getAndSet(thread);

        // если у нас нет предшественников или ПнП (предшественник нашего предшественника) доступен, то лок можно захватить
        if(generation == null || generation.predecessor == AVAILABLE) {
            return true;
        }

        // если мы не встретим доступную ноду, то лок захватить не удасться
        while (System.nanoTime() - startTime < patience) {
            LockNode nextGeneration = generation.predecessor;

            // тоже условие что в 34, кто-то мог его исполнить пока мы дошли
            if (nextGeneration == AVAILABLE) {
                return true; // <---- единственная точка выхода
            } else if (nextGeneration != null) {
                generation = nextGeneration;
            }
        }

        if (!tail.compareAndSet(thread, generation)) {
            thread.predecessor = generation;
        }

        return false;
    }

    @Override
    public void unlock() {
        LockNode thead = threadNode.get();

        // если в хвосте все еще мы (первые и единственные), то ничего не делай
        // иначе проставь в предшественника AVAILABLE
        // ЭТО ЕДИНСТВЕННОЕ МЕСТО ГДЕ ПРОСТАВЛЯЕТСЯ ПРЕДШЕСТВЕННИК - поэтому в цикле 40 следующее поколение всегда будет либо Null, либо AVAILABLE
        if(!tail.compareAndSet(thead, null)) {
            thead.predecessor = AVAILABLE;
        }
    }

    private static class LockNode {
        private volatile LockNode predecessor = null;
    }
}
