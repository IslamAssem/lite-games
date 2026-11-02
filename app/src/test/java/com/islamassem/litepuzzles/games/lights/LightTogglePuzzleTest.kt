package com.islamassem.litepuzzles.games.lights

import com.islamassem.litepuzzles.engine.PuzzleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class LightTogglePuzzleTest {

    @Test
    fun solverSolvesRandomBoards() {
        val random = Random(1)
        repeat(50) {
            val puzzle = LightTogglePuzzle.random(5, presses = 10, random = random)
            val solution = puzzle.solve(puzzle.initialState)!!
            val end = solution.fold(puzzle.initialState, puzzle::apply)
            assertEquals(PuzzleStatus.Won, puzzle.status(end))
        }
    }

    @Test
    fun solverIsMinimal_comparedWithBruteForceOn3x3() {
        val probe = LightTogglePuzzle(3, 0L)
        for (start in 1L until (1L shl 9)) {
            val puzzle = LightTogglePuzzle(3, start)
            // Brute force: smallest set of presses that clears the board.
            val best = (0 until (1 shl 9))
                .filter { set -> (0 until 9).fold(start) { b, c -> if ((set shr c) and 1 == 1) b xor probe.pattern(c) else b } == 0L }
                .minOf { Integer.bitCount(it) }
            assertEquals("start=$start", best, puzzle.solve(puzzle.initialState)?.size)
        }
    }

    @Test
    fun allLightsOn5x5_needsFifteenPresses() {
        val puzzle = LightTogglePuzzle(5, (1L shl 25) - 1)
        assertEquals(15, puzzle.solve(puzzle.initialState)?.size)
    }

    @Test
    fun some5x5PatternsAreUnsolvable() {
        // A single lit corner can't be cleared on 5×5 (the press matrix has a 2-dimensional kernel).
        val puzzle = LightTogglePuzzle(5, 1L)
        assertNull(puzzle.solve(puzzle.initialState))
    }

    @Test
    fun pressFlipsCellAndNeighbours() {
        val puzzle = LightTogglePuzzle(5, 0L)
        val after = puzzle.apply(puzzle.initialState, 12) // centre
        assertEquals(setOf(7, 11, 12, 13, 17), (0 until 25).filter { after.isOn(it) }.toSet())
    }
}
