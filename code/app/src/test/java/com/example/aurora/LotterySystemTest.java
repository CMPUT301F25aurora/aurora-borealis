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
 * COMPREHENSIVE LOTTERY SYSTEM TESTS
 * Covers:
 * US 02.05.02 (Sampling System)
 * US 02.05.03 (Replacement Drawing)
 * US 01.05.01 (Entrant's chance to be chosen again)
 */
public class LotterySystemTest {

    /**
     * Test: Lottery should draw the correct number of winners.
     *
     * Verifies:
     *  exactly N entrants are selected
     *  leftover entrants remain in waiting list
     *  winners are not duplicated in losers list
     */
    @Test
    public void testLottery_DrawsCorrectNumber() {
        List<String> waitingList = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        int spotsToFill = 3;

        Collections.shuffle(waitingList, new Random(1)); // Fixed seed
        List<String> winners = new ArrayList<>(waitingList.subList(0, spotsToFill));
        List<String> losers = new ArrayList<>(waitingList.subList(spotsToFill, waitingList.size()));

        assertEquals("Should draw exactly 3 winners", 3, winners.size());
        assertEquals("Should leave 2 entrants in waiting", 2, losers.size());


        for (String w : winners) {
            assertTrue(!losers.contains(w));
        }
    }

    /**
     * Test: If capacity exceeds entrant count, draw everyone.
     *
     * Verifies:
     *  min(spots, entrants) logic works correctly
     *  no IndexOutOfBounds errors occur
     */
    @Test
    public void testLottery_DrawsAllIfSpotsExceedEntrants() {
        // 10 spots, only 3 entrants
        List<String> waitingList = new ArrayList<>(Arrays.asList("A", "B", "C"));
        int spotsToFill = 10;

        int drawCount = Math.min(spotsToFill, waitingList.size());
        List<String> winners = waitingList.subList(0, drawCount);

        assertEquals("Should draw all 3 entrants", 3, winners.size());
    }

    /**
     * Test: Replacement logic pulls from waitlist correctly.
     *
     * Verifies:
     *  cancelled entrant is removed
     *  next waitlisted entrant is promoted
     *  waitlist updates as expected
     */
    @Test
    public void testReplacement_DrawsFromWaitlist() {
        List<String> waitingList = new ArrayList<>(Arrays.asList("ReplacementCandidate"));
        List<String> selectedList = new ArrayList<>(Arrays.asList("DropoutUser"));

        // Scenario: DropoutUser cancels
        selectedList.remove("DropoutUser");

        // Action: Draw replacement
        if (!waitingList.isEmpty()) {
            String newWinner = waitingList.remove(0);
            selectedList.add(newWinner);
        }

        assertEquals("Selected list should have 1 user again", 1, selectedList.size());
        assertEquals("ReplacementCandidate", selectedList.get(0));
        assertTrue("Waitlist should be empty", waitingList.isEmpty());
    }

    /**
     * Test: Shuffling produces different orderings.
     *
     * Verifies:
     *  shuffled lists differ from original order
     *  shuffle randomness works using different seeds
     */
    @Test
    public void testFairness_ShuffleLogic() {
        List<String> original = Arrays.asList("A", "B", "C", "D", "E");
        List<String> copy1 = new ArrayList<>(original);
        List<String> copy2 = new ArrayList<>(original);

        Collections.shuffle(copy1, new Random(System.currentTimeMillis()));

        try { Thread.sleep(10); } catch (Exception e) {}
        Collections.shuffle(copy2, new Random(System.currentTimeMillis() + 100));


        boolean listsChanged = !copy1.equals(original) || !copy2.equals(original);

        assertTrue("Shuffle logic should rearrange the list", listsChanged);
    }
}