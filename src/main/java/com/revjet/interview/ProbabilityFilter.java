package com.revjet.interview;

import java.util.function.Predicate;

public class ProbabilityFilter implements Predicate<Prioritized> {

    private final double threshold;
    private double probability;

    public ProbabilityFilter(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean test(Prioritized next) {
        boolean greaterBefore = probability >= threshold;
        probability = probability + next.getPriority();
        return (probability >= threshold) ^ greaterBefore;
    }
}
