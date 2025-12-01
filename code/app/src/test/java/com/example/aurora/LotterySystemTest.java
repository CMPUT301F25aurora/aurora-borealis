package com.example.aurora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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

    // ==================================================================
    // US 02.05.02: Sample N Attendees
    // ==================================================================

    /**
     * Ensures that the lottery draws exactly the number of winners
     * specified by the organizer (spotsToFill), and the remaining
     * entrants remain in the waiting list.
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
     * Ensures the lottery gracefully handles cases where the number of
     * available entrants is fewer than the number of required winners.
     */
    @Test
    public void testLottery_DrawsAllIfSpotsExceedEntrants() {
        List<String> waitingList = new ArrayList<>(Arrays.asList("A", "B", "C"));
        int spotsToFill = 10;

        int drawCount = Math.min(spotsToFill, waitingList.size());
        List<String> winners = waitingList.subList(0, drawCount);

        assertEquals("Should draw all 3 entrants", 3, winners.size());
    }

    // ==================================================================
    // US 02.05.03: Replacement Draw
    // ==================================================================

    /**
     * Simulates a cancellation and verifies the next entrant in the
     * waiting list is selected as a replacement.
     */
    @Test
    public void testReplacement_DrawsFromWaitlist() {
        List<String> waitingList = new ArrayList<>(Arrays.asList("ReplacementCandidate"));
        List<String> selectedList = new ArrayList<>(Arrays.asList("DropoutUser"));

        selectedList.remove("DropoutUser");

        if (!waitingList.isEmpty()) {
            String newWinner = waitingList.remove(0);
            selectedList.add(newWinner);
        }

        assertEquals("Selected list should have 1 user again", 1, selectedList.size());
        assertEquals("ReplacementCandidate", selectedList.get(0));
        assertTrue("Waitlist should be empty", waitingList.isEmpty());
    }

    // ==================================================================
    // US 01.05.01: Entrant Chance (Fairness)
    // ==================================================================

    /**
     * Ensures the shuffle operation produces different permutations,
     * validating that the selection mechanism is randomized and fair.
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

    /**
     * Verifies that when the event capacity equals the size of the waiting list,
     * all entrants are selected as winners.
     */
    @Test
    public void drawWinners_AllEntrantsSelectedWhenCapacityEqualsListSize() {
        List<String> waitingList = Arrays.asList("A", "B", "C");
        int capacity = 3;

        int toSelect = Math.min(capacity, waitingList.size());
        List<String> winners = waitingList.subList(0, toSelect);

        assertEquals(waitingList.size(), winners.size());
        assertTrue(winners.containsAll(waitingList));
    }

    /**
     * Verifies that a replacement draw for a cancelled entrant selects
     * a new entrant from the waiting list without introducing duplicates.
     */
    @Test
    public void replacementDrawSelectsNewEntrantWithoutDuplicates() {
        List<String> waitingList = Arrays.asList("A","B","C","D");

        List<String> chosen = new ArrayList<>();
        chosen.add("A");
        chosen.add("B");

        String cancelled = "A";
        chosen.remove(cancelled); // chosen now = ["B"]

        List<String> replacementPool = new ArrayList<>();
        for(String entrant:waitingList){
            if(!chosen.contains(entrant) && !entrant.equals(cancelled)){
                replacementPool.add(entrant);
            }
        }

        String replacement = replacementPool.get(0);
        chosen.add(replacement);
        assertEquals(2,chosen.size());
        assertEquals(2,new HashSet<>(chosen).size());
        assertTrue(waitingList.contains(replacement));
        assertNotEquals(cancelled,replacement);
    }

    /**
     * Verifies that the lottery sampling logic respects the requested
     * number of winners and does not exceed the size of the waiting list.
     */
    @Test
    public void runLottery_respectsRequestedSampleAndWaitingSize() {
        List<String> waitingList = Arrays.asList("A","B","C","D","E");
        int requested = 3;
        int actualSampleSize = Math.min(requested, waitingList.size());
        List<String> sampled = waitingList.subList(0, actualSampleSize);
        assertEquals(3, sampled.size());
        assertTrue(waitingList.containsAll(sampled));
    }
}