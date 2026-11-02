package com.islamassem.litepuzzles.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * One play-through of a [Puzzle]: current state, move history with undo/redo, and hints.
 * Backed by Compose snapshot state so UI recomposes automatically.
 */
class PuzzleSession<S : Any, M : Any>(val puzzle: Puzzle<S, M>) {

    /** history[0] is the initial state; history[index] is the current state. */
    private var history by mutableStateOf(listOf(puzzle.initialState))
    private var index by mutableStateOf(0)

    val state: S get() = history[index]
    val status: PuzzleStatus get() = puzzle.status(state)
    val moveCount: Int get() = index

    /** True while [playAnimated] runs; history controls are locked so the board can't change mid-animation. */
    var isAnimating by mutableStateOf(false)
        private set

    val canUndo: Boolean get() = index > 0 && !isAnimating
    val canRedo: Boolean get() = index < history.lastIndex && !isAnimating
    val canHint: Boolean get() = status == PuzzleStatus.Playing && !isAnimating

    /** Fewest moves needed to win from the start; null if not solvable within solver limits. */
    val optimalMoveCount: Int? by lazy { puzzle.solve(puzzle.initialState)?.size }

    /** Applies [move] if the game is still in progress. Discards any redo history. */
    fun play(move: M) {
        if (status != PuzzleStatus.Playing) return
        history = history.subList(0, index + 1) + puzzle.apply(state, move)
        index++
    }

    /**
     * Runs the board's [animate] (e.g. the boat rowing across) *before* committing [move], so the
     * state — and any win/lose dialog — only changes once the animation has finished.
     * If the animation is cancelled (player leaves the screen) the move is not played.
     */
    suspend fun playAnimated(move: M, animate: suspend () -> Unit) {
        if (isAnimating || status != PuzzleStatus.Playing) return
        isAnimating = true
        try {
            animate()
            play(move)
        } finally {
            isAnimating = false
        }
    }

    fun undo() {
        if (canUndo) index--
    }

    fun redo() {
        if (canRedo) index++
    }

    fun restart() {
        if (isAnimating) return
        history = listOf(puzzle.initialState)
        index = 0
    }

    /** Next move of a shortest solution from the current state, or null if none exists. */
    fun hint(): M? = if (status == PuzzleStatus.Playing) puzzle.solve(state)?.firstOrNull() else null

    /** 3 stars for an optimal win, 2 for up to 50% extra moves, otherwise 1. */
    fun stars(): Int {
        val best = optimalMoveCount ?: return 3
        return when {
            moveCount <= best -> 3
            moveCount <= best + best / 2 -> 2
            else -> 1
        }
    }
}
