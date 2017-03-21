package com.revjet.interview;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.revjet.interview.Combiner.CombinerInput;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class CombinerImplTest {

    @Test
    public void valuesFromBothInputsShouldBeInResult() throws Exception {
        Combiner<Integer> combiner = new CombinerImpl<>();
        CombinerInput<Integer> firstInput = combiner.addInput(1, 0, TimeUnit.MILLISECONDS);
        firstInput.put(1);

        CombinerInput<Integer> secondInput = combiner.addInput(1, 0, TimeUnit.MILLISECONDS);
        secondInput.put(2);

        Set<Integer> integers = Stream.of(1, 2)
            .map(i -> combiner.poll())
            .collect(Collectors.toSet());

        assertThat(integers, contains(1, 2));
    }

    @Test
    public void inputIsRemovedByTimeout() throws Exception {
        Combiner<Integer> combiner = new CombinerImpl<>();
        CombinerInput<Integer> firstInput = combiner.addInput(1, 100, TimeUnit.MILLISECONDS);
        TimeUnit.MILLISECONDS.sleep(111);
        assertThat(firstInput.isRemoved(), is(true));
    }

    @Test
    public void elementsShouldBeUniformlyDistributed() throws Exception {
        Combiner<Integer> combiner = new CombinerImpl<>();
        CombinerInput<Integer> oddInput = combiner.addInput(1, 0, TimeUnit.MILLISECONDS);

        Stream.iterate(0, i -> i + 1)
            .filter(i -> i % 2 == 0)
            .limit(1000)
            .forEach(oddInput::put);

        CombinerInput<Integer> evenInput = combiner.addInput(1, 0, TimeUnit.MILLISECONDS);

        Stream.iterate(0, i -> i + 1)
            .filter(i -> i % 2 != 0)
            .limit(1000)
            .forEach(evenInput::put);

        Map<Boolean, Long> collect = Stream.generate(combiner::poll)
            .limit(1000)
            .collect(Collectors.groupingBy((i) -> i % 2 == 0, Collectors.counting()));

        long dispersion = Math.abs(collect.get(true) - collect.get(false));

        assertThat(dispersion, lessThan(100L));

    }

    @Test
    public void prioritizedShouldAppearMoreFrequently() throws Exception {
        Combiner<Integer> combiner = new CombinerImpl<>();
        CombinerInput<Integer> oddInput = combiner.addInput(2, 0, TimeUnit.MILLISECONDS);

        Stream.iterate(0, i -> i + 1)
            .filter(i -> i % 2 == 0)
            .limit(1000)
            .forEach(oddInput::put);

        CombinerInput<Integer> evenInput = combiner.addInput(1, 0, TimeUnit.MILLISECONDS);

        Stream.iterate(0, i -> i + 1)
            .filter(i -> i % 2 != 0)
            .limit(1000)
            .forEach(evenInput::put);

        Map<Boolean, Long> parityToCount = Stream.generate(combiner::poll)
            .limit(1000)
            .collect(Collectors.groupingBy((i) -> i % 2 == 0, Collectors.counting()));

        long dispersion = Math.abs(parityToCount.get(true) - parityToCount.get(false));

        assertThat(dispersion, lessThan(500L));
        assertThat(dispersion, greaterThan(250L));

    }

    @Test
    public void shouldGetElementsFromLessPrioritizedQueueIfOtherAreEmpty() throws Exception {
        Combiner<Integer> combiner = new CombinerImpl<>();
        CombinerInput<Integer> prioritizedEmptyInput = combiner.addInput(2, 0, TimeUnit.MILLISECONDS);

        CombinerInput<Integer> lessPrioritizedInput = combiner.addInput(1, 0, TimeUnit.MILLISECONDS);

        Stream.iterate(0, i -> i + 1)
            .limit(100)
            .forEach(lessPrioritizedInput::put);

        List<Integer> result = Stream.generate(combiner::poll)
            .limit(100)
            .collect(Collectors.toList());

        assertThat(result, not(contains(nullValue())));
        assertThat(result, hasSize(100));
    }

    @Test
    public void pollWithTimeoutShouldReturnValue() throws Exception {
        Combiner<Integer> combiner = new CombinerImpl<>();
        CombinerInput<Integer> firstInput = combiner.addInput(1, 0, TimeUnit.MILLISECONDS);

        CompletableFuture.runAsync(
            () -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                firstInput.put(1);
            });

        Integer value = combiner.poll(20, TimeUnit.MILLISECONDS);

        assertThat(value, not(nullValue()));
    }

    @Test
    public void nullOnTimeOut() throws Exception {
        Combiner<Integer> combiner = new CombinerImpl<>();
        CombinerInput<Integer> firstInput = combiner.addInput(1, 0, TimeUnit.MILLISECONDS);
        CompletableFuture.runAsync(
            () -> {
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                firstInput.put(1);
            });

        Integer value = combiner.poll(20, TimeUnit.MILLISECONDS);

        assertThat(value, nullValue());
    }
}
