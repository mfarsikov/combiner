package com.revjet.interview;

public class TimedOutCollector {

    private final Thread thread;

    public TimedOutCollector(Runnable checkTimeOut) {
        thread = new Thread(() -> {
            while (!Thread.interrupted()) {
                checkTimeOut.run();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    protected void finalize() {
        thread.interrupt();
    }
}
