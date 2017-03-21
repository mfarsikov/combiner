package com.revjet.interview;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class ProbabilityFilterTest {

    @Test
    public void test1() throws Exception {
        Prioritized element =  mock(Prioritized.class);
        when(element.getPriority()).thenReturn(2.0);

        ProbabilityFilter probabilityFilter = new ProbabilityFilter(3);

        assertThat(probabilityFilter.test(element), is(false));
        assertThat(probabilityFilter.test(element), is(true));
    }

    @Test
    public void test2() throws Exception {
        Prioritized element =  mock(Prioritized.class);
        when(element.getPriority()).thenReturn(2.0);

        ProbabilityFilter probabilityFilter = new ProbabilityFilter(6);

        assertThat(probabilityFilter.test(element), is(false));
        assertThat(probabilityFilter.test(element), is(false));
        assertThat(probabilityFilter.test(element), is(true));


    }

    @Test
    public void onlyOnePass() throws Exception {
        Prioritized element = mock(Prioritized.class);
        when(element.getPriority()).thenReturn(2.0);

        ProbabilityFilter probabilityFilter = new ProbabilityFilter(4);

        assertThat(probabilityFilter.test(element), is(false));
        assertThat(probabilityFilter.test(element), is(true));
        assertThat(probabilityFilter.test(element), is(false));
    }
}
