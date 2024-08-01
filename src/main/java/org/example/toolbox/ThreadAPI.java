package org.example.toolbox;

import org.example.stage2.peterson.PetersonThread;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.TimeUnit;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ThreadAPI {

    public static int threadID(){
        Thread thread = Thread.currentThread();

        if(thread instanceof PetersonThread petersonThread)
            return petersonThread.getPetersonID();

        return (int) thread.threadId();
    }

    /**
     * Метод позволяет вызвать sleep у потока не выбрасывая исключения, но сохраняя поток прерванным
     *
     * @param amount количество времени, на которое требуется заснуть
     * @param unit единицы времени
     */
    public static void sleep(long amount, TimeUnit unit) {
        try {

            unit.sleep(amount);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
