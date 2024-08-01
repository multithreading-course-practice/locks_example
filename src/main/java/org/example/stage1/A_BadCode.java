package org.example.stage1;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class A_BadCode {

    public volatile boolean suspend = true;

    private int v; //значения не синхронизированы - поведение непредсказуемо
    private int x;

    public void release(int value) {
        while (suspend) { Thread.onSpinWait(); }

        this.x = 10;
        this.x = x + 1;
        this.x = x + 1;
        v = value;
    }

    public void acquire() {
        while (suspend) { Thread.onSpinWait(); }

        int vValue = v;
        int xValue = x;
        log.info("v = {}, x = {}", vValue, xValue);
    }

    public static void main(String[] args) {

        Runnable command = () -> {
            final A_BadCode show = new A_BadCode();

            Runnable release = () -> show.release(1);
            Runnable acquire = show::acquire;

            new Thread(release).start();
            new Thread(acquire).start();

            show.suspend = false;
        };

        for (int i = 0; i < 1000; i++) {
            new Thread(command).start();
        }
    }
}
