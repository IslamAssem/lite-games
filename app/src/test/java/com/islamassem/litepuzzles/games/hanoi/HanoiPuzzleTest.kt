package com.islamassem.litepuzzles.games.hanoi

import com.islamassem.litepuzzles.engine.PuzzleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HanoiPuzzleTest {

    @Test
    fun shortestSolutionIsTwoToTheNMinusOne() {
        for (n in 1..6) {
            val puzzle = HanoiPuzzle(n)
            assertEquals("n=$n", (1 shl n) - 1, puzzle.solve(puzzle.initialState)?.size)
        }
    }

    @Test
    fun cannotPlaceBiggerDiscOnSmaller() {
        val puzzle = HanoiPuzzle(3)
        val s = puzzle.apply(puzzle.initialState, HanoiMove(0, 2)) // disc 1 -> right
        assertFalse(puzzle.isLegal(s, HanoiMove(0, 2)))            // disc 2 onto disc 1
        assertTrue(puzzle.isLegal(s, HanoiMove(0, 1)))
        assertFalse(puzzle.isLegal(s, HanoiMove(1, 0)))            // empty peg
    }

    @Test
    fun wholeTowerOnRightPegWins() {
        val puzzle = HanoiPuzzle(2)
        assertEquals(PuzzleStatus.Won, puzzle.status(HanoiState(listOf(emptyList(), emptyList(), listOf(2, 1)))))
        assertEquals(PuzzleStatus.Playing, puzzle.status(HanoiState(listOf(emptyList(), listOf(2, 1), emptyList()))))
    }
}
