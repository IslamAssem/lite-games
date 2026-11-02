package com.islamassem.litepuzzles.games.rivercrossing

import com.islamassem.litepuzzles.engine.PuzzleSession
import com.islamassem.litepuzzles.engine.PuzzleStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiverCrossingPuzzleTest {

    @Test
    fun farmerWolfGoatCabbage_shortestSolutionIsSevenCrossings() {
        val puzzle = RiverCrossingLevels.farmerWolfGoatCabbagePuzzle()
        val solution = puzzle.solve(puzzle.initialState)!!
        assertEquals(7, solution.size)
        // First move must be farmer + goat.
        assertEquals(setOf("farmer", "goat"), solution.first().passengerIds)
    }

    @Test
    fun missionariesAndCannibals_shortestSolutionIsElevenCrossings() {
        val puzzle = RiverCrossingLevels.missionariesAndCannibalsPuzzle()
        assertEquals(11, puzzle.solve(puzzle.initialState)?.size)
    }

    @Test
    fun leavingWolfWithGoat_loses() {
        val puzzle = RiverCrossingLevels.farmerWolfGoatCabbagePuzzle()
        val next = puzzle.apply(puzzle.initialState, Crossing(setOf("farmer", "cabbage")))
        assertEquals(PuzzleStatus.Lost("The wolf ate the goat!"), puzzle.status(next))
    }

    @Test
    fun onlyRowersCanTakeTheBoat() {
        val puzzle = RiverCrossingLevels.farmerWolfGoatCabbagePuzzle()
        val moves = puzzle.legalMoves(puzzle.initialState)
        assertTrue(moves.all { "farmer" in it.passengerIds })
        assertFalse(puzzle.isLegal(puzzle.initialState, Crossing(setOf("goat"))))
        assertFalse(puzzle.isLegal(puzzle.initialState, Crossing(setOf("farmer", "goat", "wolf"))))
    }

    @Test
    fun playAnimated_commitsMoveOnlyAfterAnimation_andLocksControlsMeanwhile() = runBlocking {
        val session = PuzzleSession(RiverCrossingLevels.farmerWolfGoatCabbagePuzzle())
        val move = Crossing(setOf("farmer", "goat"))

        session.playAnimated(move) {
            assertEquals("state must not change mid-animation", 0, session.moveCount)
            assertTrue(session.isAnimating)
            assertFalse(session.canHint)
        }
        assertEquals(1, session.moveCount)
        assertFalse(session.isAnimating)
        assertTrue(session.canUndo)
    }

    @Test
    fun session_playingTheSolutionWinsWithThreeStars_andUndoRedoWork() {
        val puzzle = RiverCrossingLevels.farmerWolfGoatCabbagePuzzle()
        val session = PuzzleSession(puzzle)
        val solution = puzzle.solve(puzzle.initialState)!!

        session.play(solution[0])
        session.undo()
        assertEquals(0, session.moveCount)
        session.redo()
        assertEquals(1, session.moveCount)

        // Following hints from here must finish optimally.
        while (session.status == PuzzleStatus.Playing) session.play(session.hint()!!)
        assertEquals(solution.size, session.moveCount)
        assertEquals(PuzzleStatus.Won, session.status)
        assertEquals(3, session.stars())

        session.restart()
        assertEquals(PuzzleStatus.Playing, session.status)
        assertFalse(session.canUndo)
    }
}
