package com.revjet.interview;

import com.revjet.interview.Combiner.CombinerInput;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class Input<T> implements CombinerInput<T>, Prioritized {

    private final ConcurrentLinkedQueue<T> queue;
    private final Double priority;
    private final Long nanoTimeout;
    private final Consumer<Input<T>> onRemove;
    private final Runnable timeUpdate;
    private volatile Instant removeTime = Instant.MAX;
    private boolean removed;

    public Input(Double priority, Long nanoTimeout, Consumer<Input<T>> onRemove) {
        this.queue = new ConcurrentLinkedQueue<>();
        this.priority = priority;
        this.onRemove = onRemove;
        this.nanoTimeout = nanoTimeout;

        if (nanoTimeout > 0) {
            timeUpdate = this::updateRemoveTime;
        } else {
            timeUpdate = () -> {};
        }

        timeUpdate.run();
    }

    private void updateRemoveTime() {
        removeTime = Instant.now().plus(nanoTimeout, ChronoUnit.NANOS);
    }

    @Override
    public void put(T value) {
        timeUpdate.run();
        queue.offer(value);
    }

    @Override
    public synchronized void remove() {
        if (!removed) {
            removed = true;
            onRemove.accept(this);
        }
    }

    @Override
    public synchronized boolean isRemoved() {
        return removed;
    }

    public void checkTimeout(Instant instant) {
        if (instant.isAfter(removeTime)) {
            remove();
        }
    }

    public ConcurrentLinkedQueue<T> getQueue() {
        return queue;
    }

    @Override
    public Double getPriority() {
        return priority;
    }

}
