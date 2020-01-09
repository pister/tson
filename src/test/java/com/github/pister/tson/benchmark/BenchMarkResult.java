package com.github.pister.tson.benchmark;

/**
 * Created by songlihuang on 2020/1/9.
 */
public class BenchMarkResult {

    private long totalEscapeTimesMs;

    public BenchMarkResult(long totalEscapeTimesMs) {
        this.totalEscapeTimesMs = totalEscapeTimesMs;
    }

    public long getTotalEscapeTimesMs() {
        return totalEscapeTimesMs;
    }

    public void setTotalEscapeTimesMs(long totalEscapeTimesMs) {
        this.totalEscapeTimesMs = totalEscapeTimesMs;
    }
}
