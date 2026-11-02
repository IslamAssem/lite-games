package com.islamassem.litepuzzles.games.pouring

import com.islamassem.litepuzzles.engine.PuzzleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WaterJugsPuzzleTest {

    private val puzzle = PouringLevels.waterJugsPuzzle()

    @Test
    fun measureFourLitres_shortestSolutionIsSixMoves() {
        assertEquals(6, puzzle.solve(puzzle.initialState)?.size)
    }

    @Test
    fun pourStopsWhenTargetIsFull() {
        val full5 = puzzle.apply(puzzle.initialState, JugMove.Fill(1)) // [0, 5]
        val poured = puzzle.apply(full5, JugMove.Pour(from = 1, to = 0))
        assertEquals(listOf(3, 2), poured.amounts)
    }

    @Test
    fun pourStopsWhenSourceIsEmpty() {
        val state = JugsState(listOf(3, 0))
        assertEquals(listOf(0, 3), puzzle.apply(state, JugMove.Pour(0, 1)).amounts)
    }

    @Test
    fun pointlessMovesAreNotOffered() {
        val moves = puzzle.legalMoves(puzzle.initialState)
        assertEquals(setOf(JugMove.Fill(0), JugMove.Fill(1)), moves.toSet())
        assertFalse(puzzle.isLegal(puzzle.initialState, JugMove.Pour(0, 1)))
    }

    @Test
    fun anyJugWithFourLitresWins() {
        assertEquals(PuzzleStatus.Won, puzzle.status(JugsState(listOf(0, 4))))
        assertEquals(PuzzleStatus.Playing, puzzle.status(JugsState(listOf(3, 2))))
    }
}
