package com.islamassem.litepuzzles.games.pouring

import com.islamassem.litepuzzles.engine.Puzzle
import com.islamassem.litepuzzles.engine.PuzzleStatus

data class JugsState(val amounts: List<Int>)

sealed interface JugMove {
    data class Fill(val jug: Int) : JugMove
    data class Empty(val jug: Int) : JugMove
    data class Pour(val from: Int, val to: Int) : JugMove
}

/**
 * Generic jug-pouring puzzle. Pouring stops when the source is empty or the target is full.
 * With [tapAndDrain] the player may also fill a jug from the tap or empty it onto the ground
 * (classic "measure 4 L"); without it, only pouring between jugs is allowed ("split the wine").
 * Pointless moves (filling a full jug, pouring from an empty one…) are not offered.
 */
class WaterJugsPuzzle(
    val capacities: List<Int>,
    start: List<Int> = capacities.map { 0 },
    val tapAndDrain: Boolean,
    /** Shown to the player, e.g. "Measure exactly 4 L". */
    val goalText: String,
    private val isGoal: (List<Int>) -> Boolean,
) : Puzzle<JugsState, JugMove> {

    init {
        require(start.size == capacities.size) { "One start amount per jug" }
        require(start.indices.all { start[it] in 0..capacities[it] }) { "Start amounts must fit the jugs" }
    }

    override val initialState = JugsState(start)

    override fun legalMoves(state: JugsState): List<JugMove> {
        val a = state.amounts
        val moves = mutableListOf<JugMove>()
        for (i in a.indices) {
            if (tapAndDrain && a[i] < capacities[i]) moves += JugMove.Fill(i)
            if (tapAndDrain && a[i] > 0) moves += JugMove.Empty(i)
            for (j in a.indices) {
                if (i != j && a[i] > 0 && a[j] < capacities[j]) moves += JugMove.Pour(i, j)
            }
        }
        return moves
    }

    fun isLegal(state: JugsState, move: JugMove) = move in legalMoves(state)

    override fun apply(state: JugsState, move: JugMove): JugsState {
        val a = state.amounts.toMutableList()
        when (move) {
            is JugMove.Fill -> a[move.jug] = capacities[move.jug]
            is JugMove.Empty -> a[move.jug] = 0
            is JugMove.Pour -> {
                val amount = minOf(a[move.from], capacities[move.to] - a[move.to])
                a[move.from] -= amount
                a[move.to] += amount
            }
        }
        return JugsState(a)
    }

    override fun status(state: JugsState): PuzzleStatus =
        if (isGoal(state.amounts)) PuzzleStatus.Won else PuzzleStatus.Playing

    fun jugName(i: Int) = "${capacities[i]} L jug"

    override fun describe(move: JugMove): String = when (move) {
        is JugMove.Fill -> "Fill the ${jugName(move.jug)} from the tap"
        is JugMove.Empty -> "Empty the ${jugName(move.jug)}"
        is JugMove.Pour -> "Pour the ${jugName(move.from)} into the ${jugName(move.to)}"
    }
}
