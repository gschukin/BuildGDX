package ru.m210projects.Build;

public class Timer {

    protected final int timerfreq;
    protected final int timerticspersec;
    protected long timerlastsample;
    protected int totalclock;

    public Timer(int tickspersecond) {
        this.totalclock = 0;
        this.timerfreq = 1000;
        this.timerticspersec = tickspersecond;
        this.timerlastsample = System.nanoTime() * timerticspersec / (timerfreq * 1000000L);
    }

    public void update() {
        long n = (System.nanoTime() * timerticspersec / (timerfreq * 1000000L)) - timerlastsample;
        if (n > 0) {
            totalclock += n;
            timerlastsample += n;
        }
    }

    public int getFreq() {
        return timerfreq;
    }

    public int getTicsPerSecond() {
        return timerticspersec;
    }

    public void reset() {
        totalclock = 0;
    }

    public void calcLag(int lag) {
        totalclock -= lag;
    }

    public int getTotalClock() {
        return totalclock;
    }

    public void setTotalClock(int totalclock) {
        this.totalclock = totalclock;
    }
}
