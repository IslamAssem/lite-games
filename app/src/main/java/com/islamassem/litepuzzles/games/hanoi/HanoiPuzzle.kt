package com.islamassem.litepuzzles.games.hanoi

import com.islamassem.litepuzzles.engine.Puzzle
import com.islamassem.litepuzzles.engine.PuzzleStatus

/** Each peg lists disc sizes from bottom to top; size 1 is the smallest disc. */
data class HanoiState(val pegs: List<List<Int>>) {
    fun top(peg: Int): Int? = pegs[peg].lastOrNull()
}

data class HanoiMove(val from: Int, val to: Int)

/**
 * Tower of Hanoi with [discCount] discs on three pegs: move the whole tower from the left
 * peg to the right peg, one disc at a time, never placing a disc on a smaller one.
 * Illegal moves are simply not offered, so the game can't be lost.
 * Shortest solution is 2^n − 1 moves; BFS handles it comfortably up to ~8 discs.
 */
class HanoiPuzzle(val discCount: Int) : Puzzle<HanoiState, HanoiMove> {

    init {
        require(discCount in 1..8) { "1..8 discs supported" }
    }

    override val initialState = HanoiState(listOf((discCount downTo 1).toList(), emptyList(), emptyList()))

    override fun legalMoves(state: HanoiState): List<HanoiMove> =
        PEGS.flatMap { from -> PEGS.map { to -> HanoiMove(from, to) } }.filter { isLegal(state, it) }

    fun isLegal(state: HanoiState, move: HanoiMove): Boolean {
        if (move.from == move.to) return false
        val disc = state.top(move.from) ?: return false
        val target = state.top(move.to)
        return target == null || target > disc
    }

    override fun apply(state: HanoiState, move: HanoiMove): HanoiState {
        require(isLegal(state, move)) { "Illegal move $move" }
        val pegs = state.pegs.map { it.toMutableList() }
        pegs[move.to].add(pegs[move.from].removeAt(pegs[move.from].lastIndex))
        return HanoiState(pegs)
    }

    override fun status(state: HanoiState): PuzzleStatus =
        if (state.pegs[PEGS.last].size == discCount) PuzzleStatus.Won else PuzzleStatus.Playing

    override fun describe(move: HanoiMove) = "Move the top disc from ${PEG_NAMES[move.from]} to ${PEG_NAMES[move.to]}"

    companion object {
        val PEGS = 0..2
        val PEG_NAMES = listOf("Left", "Middle", "Right")
    }
}
