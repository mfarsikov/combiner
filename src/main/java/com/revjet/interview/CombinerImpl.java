package com.revjet.interview;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class CombinerImpl<T> implements Combiner<T> {

    private final Random random;
    private final ConcurrentLinkedQueue<Input<T>> inputs;
    private final TimedOutCollector timedOutCollector;
    private volatile double totalPriority;

    public CombinerImpl() {

        inputs = new ConcurrentLinkedQueue<>();
        random = new Random();
        timedOutCollector = new TimedOutCollector(this::checkTimedOut);
    }

    private void checkTimedOut() {
        Instant now = Instant.now();
        for (Input<?> input : inputs) {
            input.checkTimeout(now);
        }
    }

    @Override
    public CombinerInput<T> addInput(double priority, long isEmptyTimeout, TimeUnit timeUnit) {

        Input<T> input = new Input<>(
            priority,
            timeUnit.toNanos(isEmptyTimeout),
            this::onInputRemove
        );

        addPriority(priority);
        inputs.add(input);
        return input;
    }

    private void onInputRemove(Input<T> removed) {
        addPriority(-removed.getPriority());
        inputs.remove(removed);
    }

    @Override
    public T poll() {
        return pickQueueWithRespectToProbability()
            .map(ConcurrentLinkedQueue::poll)
            .orElseGet(() -> pollAny()
                .orElse(null));
    }

    @Override
    public T poll(long timeout, TimeUnit timeUnit) {
        return pickQueueWithRespectToProbability()
            .map(poll(timeUnit.toNanos(timeout)))
            .orElseGet(() -> pollAny()
                .orElse(null));
    }

    private synchronized void addPriority(double priority) {
        totalPriority = totalPriority + priority;
    }


    private Optional<ConcurrentLinkedQueue<T>> pickQueueWithRespectToProbability() {
        return inputs.stream()
            .filter(new ProbabilityFilter(totalPriority * random.nextDouble()))
            .findFirst()
            .map(Input::getQueue);
    }

    private Optional<T> pollAny() {
        return inputs.stream()
            .map(Input::getQueue)
            .map(ConcurrentLinkedQueue::poll)
            .filter(Objects::nonNull)
            .findFirst();
    }

    private Function<ConcurrentLinkedQueue<T>, T> poll(long timeout) {
        return q -> {
            Instant end = Instant.now().plusNanos(timeout);
            T result;
            do {
                result = q.poll();
            }
            while (result == null && Instant.now().isBefore(end));
            return result;
        };
    }
}
