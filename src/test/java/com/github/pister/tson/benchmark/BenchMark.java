package com.github.pister.tson.benchmark;

/**
 * Created by songlihuang on 2020/1/9.
 */
public class BenchMark {

    private int times = 1000;

    private int threadCount = 1;

    public BenchMarkResult executeAction(final Action action) {
        Thread[] threads = new Thread[threadCount];
        long start = System.currentTimeMillis();
        for (int n = 0; n < threadCount; n++) {
            threads[n] = new Thread() {
                public void run() {
                    for (int i = 0; i < times; i++) {
                        action.run();
                    }
                }
            };
            threads[n].start();
        }
        for (int n = 0; n < threadCount; n++) {
            try {
                threads[n].join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long end = System.currentTimeMillis();
        return new BenchMarkResult(end - start);
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }
}
