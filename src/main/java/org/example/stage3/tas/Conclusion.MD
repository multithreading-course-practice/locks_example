From the point of view of correctness, TASLock and
TTASLock are equivalent: Each guarantees deadlock-free mutual exclusion. Under the
simple model we have been using so far, there should be no difference between these
two algorithms

BUS + Spin locks truth Page: 165-166
Главное, что в TAS потоки пытающиеся захватить блокировку загружают шину между кэшами

Добавить картинку с сайта дядьки о процессорах
