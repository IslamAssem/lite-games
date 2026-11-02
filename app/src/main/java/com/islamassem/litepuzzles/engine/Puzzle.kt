package com.islamassem.litepuzzles.engine

/**
 * A turn-based, single-player puzzle described as a state machine.
 *
 * Implementations must be pure: [apply] returns a new state and never mutates its input.
 * States are used as hash keys by the solver, so [S] should be an immutable data class
 * with value-based equals/hashCode.
 *
 * [legalMoves] should include moves that lose the game — the player must be allowed to
 * make mistakes. The solver skips states whose [status] is [PuzzleStatus.Lost].
 */
interface Puzzle<S : Any, M : Any> {
    val initialState: S

    fun legalMoves(state: S): List<M>

    fun apply(state: S, move: M): S

    fun status(state: S): PuzzleStatus

    /** Human-readable text for a move, used for hints. */
    fun describe(move: M): String = move.toString()

    /**
     * Shortest solution from [from], or null if unsolvable. Defaults to breadth-first search;
     * override for puzzles whose state space is too large for BFS (e.g. the 15-tile puzzle).
     */
    fun solve(from: S): List<M>? = BfsSolver.solve(this, from)
}

sealed interface PuzzleStatus {
    data object Playing : PuzzleStatus
    data object Won : PuzzleStatus
    data class Lost(val reason: String) : PuzzleStatus
}
