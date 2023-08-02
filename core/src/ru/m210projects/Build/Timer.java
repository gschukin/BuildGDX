package ru.m210projects.Build;

import ru.m210projects.Build.osd.Console;

public class Timer {

    protected final int timerfreq;
    protected final int timerticspersec;
    protected long timerlastsample;
    protected int totalclock;
    protected int frameTicks;
    protected int timerskipticks;
    protected float frametime;

    public Timer(int tickspersecond, int frameTicks) {
        Console.out.println("Initializing timer");
        this.totalclock = 0;
        this.timerfreq = 1000;
        this.frameTicks = frameTicks;
        this.timerticspersec = tickspersecond;
        this.timerlastsample = System.nanoTime() * timerticspersec / (timerfreq * 1000000L);

        this.timerskipticks = (timerfreq / timerticspersec) * frameTicks;
        this.frametime = 0;
    }

    public void update() {
        long n = (System.nanoTime() * timerticspersec / (timerfreq * 1000000L)) - timerlastsample;
        if (n > 0) {
            totalclock += n;
            timerlastsample += n;
        }
    }

    public int getsmoothratio(float deltaTime) {
//		return ((totalclock - game.pNet.ototalclock + ticks) << 16) / ticks;
//		return (int) (((System.nanoTime() - timernexttick) * 65536.0f) / (timerskipticks * 1000000.0f));
        return (int) ((frametime += deltaTime * 1000.0f * 65536.0f) / timerskipticks);
    }

    public void resetsmoothticks() {
        frametime = 0.0f;
    }

    public int getFrameTicks() {
        return frameTicks;
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
