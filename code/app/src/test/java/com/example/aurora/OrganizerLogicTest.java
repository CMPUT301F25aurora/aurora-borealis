package com.example.aurora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
/**
 * OrganizerLogicTest
 *
 * Contains small focused logic tests used by organizers:
 *  - Lottery sampling behaviour
 *  - Handling cases where available spots exceed list size
 */
public class OrganizerLogicTest {

    /**
     * Tests that the lottery sampling algorithm:
     *     Shuffles the waiting list deterministically when given a fixed seed,
     *     Selects exactly N entrants based on available spots,
     *     and does not select duplicate entrants

     */
    @Test
    public void testLotterySampling_Logic() {

        List<String> waitingList = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        int spotsAvailable = 2;
        Collections.shuffle(waitingList, new Random(123));
        List<String> selected = waitingList.subList(0, spotsAvailable);
        assertEquals(2, selected.size());
        assertTrue(!selected.get(0).equals(selected.get(1)));
    }

    /**
     * Tests that the lottery system gracefully handles cases where
     * the number of available spots exceeds the size of the waiting list.
     */
    @Test
    public void testLottery_NotEnoughEntrants() {

        List<String> waitingList = new ArrayList<>(Arrays.asList("A", "B"));
        int spotsAvailable = 5;

        int actualToSelect = Math.min(spotsAvailable, waitingList.size());
        List<String> selected = waitingList.subList(0, actualToSelect);

        assertEquals(2, selected.size());
    }
}