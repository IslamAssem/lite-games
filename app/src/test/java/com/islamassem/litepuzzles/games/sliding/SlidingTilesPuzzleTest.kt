package com.islamassem.litepuzzles.games.sliding

import com.islamassem.litepuzzles.engine.BfsSolver
import com.islamassem.litepuzzles.engine.PuzzleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class SlidingTilesPuzzleTest {

    @Test
    fun aStarSolutionsSolveTheBoardAndMatchBfsLength() {
        val random = Random(42)
        repeat(3) {
            val puzzle = SlidingTilesPuzzle.scrambled(3, random)
            val solution = puzzle.solve(puzzle.initialState)!!
            val end = solution.fold(puzzle.initialState, puzzle::apply)
            assertEquals(PuzzleStatus.Won, puzzle.status(end))
            val bfs = BfsSolver.solve(puzzle, puzzle.initialState, maxStates = 200_000)!!
            assertEquals(bfs.size, solution.size)
        }
    }

    @Test
    fun onlyTilesNextToTheGapCanSlide() {
        // 1 2 3 / 4 5 6 / 7 _ 8
        val puzzle = SlidingTilesPuzzle(3, listOf(1, 2, 3, 4, 5, 6, 7, 0, 8))
        val s = puzzle.initialState
        assertEquals(SlideMove(8, Direction.LEFT), puzzle.moveFor(s, 8))
        assertEquals(SlideMove(7, Direction.RIGHT), puzzle.moveFor(s, 7))
        assertEquals(SlideMove(5, Direction.DOWN), puzzle.moveFor(s, 5))
        assertNull(puzzle.moveFor(s, 1))
        assertEquals(1, puzzle.solve(s)?.size)
        assertEquals("Slide the 8 left", puzzle.describe(puzzle.solve(s)!!.single()))
    }

    @Test
    fun scramblesAreNeverTrivial() {
        val random = Random(7)
        repeat(20) {
            val p = SlidingTilesPuzzle.scrambled(3, random)
            assertEquals(PuzzleStatus.Playing, p.status(p.initialState))
        }
    }
}
