package org.example.stage2.peterson;

import org.example.toolbox.Lock;
import org.example.toolbox.ThreadAPI;

/**
 * Обычные примитивы не позволяют нам достигнуть уровня консенсуса между более чем двумя потоками, а мы бы хотели реализовывать
 * локи, с уровнем консенсуса равным бесконечности, то есть бесконечно много потоков могут захватить и зарилизить лок
 */
public class PetersonLock implements Lock {

    private volatile boolean[] flag = new boolean[2]; //потенциально нерабочий код. Нужно использовать AtomicBoolean[]
    private volatile int victim;

    @Override
    public void lock() {
        int id = ThreadAPI.threadID(); // либо 0 либо 1
        int other = 1 - id;
        flag[id] = true; // я хочу захватить блокировку
        this.victim = id; // пропускаю тебя вперед

        // пока другой хочет захватить лок И ты уступаешь - ЖДЕМ
        // почему нельзя оставить просто victim?
        while (flag[other] && victim == id) {} // spin
    }

    @Override
    public void unlock() {
        flag[victim] = false;
    }
}
