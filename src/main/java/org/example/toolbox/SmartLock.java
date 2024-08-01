package org.example.toolbox;

import java.util.concurrent.TimeUnit;

public interface SmartLock {

    boolean tryLock(long time, TimeUnit unit);

    void unlock();
}
