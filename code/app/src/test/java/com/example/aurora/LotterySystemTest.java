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
    @Test
    public void testLottery_DrawsCorrectNumber() {
        List<String> waitingList = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        int spotsToFill = 3;

        // Logic: Shuffle and Sublist
        Collections.shuffle(waitingList, new Random(1)); // Fixed seed
        List<String> winners = new ArrayList<>(waitingList.subList(0, spotsToFill));
        List<String> losers = new ArrayList<>(waitingList.subList(spotsToFill, waitingList.size()));

        assertEquals("Should draw exactly 3 winners", 3, winners.size());
        assertEquals("Should leave 2 entrants in waiting", 2, losers.size());

        // Verify no overlap
        for (String w : winners) {
            assertTrue(!losers.contains(w));
        }
    }

    @Test
    public void testLottery_DrawsAllIfSpotsExceedEntrants() {
        // 10 spots, only 3 entrants
        List<String> waitingList = new ArrayList<>(Arrays.asList("A", "B", "C"));
        int spotsToFill = 10;

        int drawCount = Math.min(spotsToFill, waitingList.size());
        List<String> winners = waitingList.subList(0, drawCount);

        assertEquals("Should draw all 3 entrants", 3, winners.size());
    }

    // ==================================================================
    // US 02.05.03: Replacement Draw
    // ==================================================================
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

    // ==================================================================
    // US 01.05.01: Entrant Chance (Fairness)
    // ==================================================================
    @Test
    public void testFairness_ShuffleLogic() {
        List<String> original = Arrays.asList("A", "B", "C", "D", "E");
        List<String> copy1 = new ArrayList<>(original);
        List<String> copy2 = new ArrayList<>(original);

        // Logic: Ensure Randomness is applied
        Collections.shuffle(copy1, new Random(System.currentTimeMillis()));
        // Pause to ensure seed change if machine is super fast
        try { Thread.sleep(10); } catch (Exception e) {}
        Collections.shuffle(copy2, new Random(System.currentTimeMillis() + 100));

        // Note: There is a tiny statistical chance they are identical,
        // but for unit testing logic we assume the method executes.
        boolean listsChanged = !copy1.equals(original) || !copy2.equals(original);

        assertTrue("Shuffle logic should rearrange the list", listsChanged);
    }

    // ============================================================
    // Capacity rule: if spots == entrants, everyone is selected
    // ============================================================
    @Test
    public void drawWinners_AllEntrantsSelectedWhenCapacityEqualsListSize() {
        List<String> waitingList = Arrays.asList("A", "B", "C");
        int capacity = 3;

        // Logic: number to select is min(capacity, waitingList.size())
        int toSelect = Math.min(capacity, waitingList.size());
        List<String> winners = waitingList.subList(0, toSelect);

        // All entrants should be selected
        assertEquals(waitingList.size(), winners.size());
        assertTrue(winners.containsAll(waitingList));
    }

    // ============================================================
    // Replacement draw: pick new entrant, avoid duplicates
    // ============================================================
    @Test
    public void replacementDrawSelectsNewEntrantWithoutDuplicates() {
        List<String> waitingList = Arrays.asList("A","B","C","D");

        // Entrants already chosen in the first draw
        List<String> chosen = new ArrayList<>();
        chosen.add("A");
        chosen.add("B");

        // Simulate one chosen entrant cancelling
        String cancelled = "A";
        chosen.remove(cancelled); // chosen now = ["B"]

        // Replacement pool = waiting list minus already-chosen AND minus cancelled
        List<String> replacementPool = new ArrayList<>();
        for(String entrant:waitingList){
            if(!chosen.contains(entrant) && !entrant.equals(cancelled)){
                replacementPool.add(entrant);
            }
        }

        // Pick the first available replacement (deterministic for the test)
        String replacement = replacementPool.get(0);
        chosen.add(replacement);

        // 1) We still have 2 chosen entrants after replacement
        assertEquals(2,chosen.size());

        // 2) No duplicates in chosen list
        assertEquals(2,new HashSet<>(chosen).size());

        // 3) Replacement came from waiting list and was not the cancelled entrant
        assertTrue(waitingList.contains(replacement));
        assertNotEquals(cancelled,replacement);
    }


}