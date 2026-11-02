package com.islamassem.litepuzzles.games.lights

import com.islamassem.litepuzzles.engine.Puzzle
import com.islamassem.litepuzzles.engine.PuzzleStatus
import kotlin.random.Random

/** Bit i set = light i (row-major) is on. Supports grids up to 8×8. */
data class LightsState(val bits: Long) {
    fun isOn(cell: Int) = (bits shr cell) and 1L == 1L
    val litCount: Int get() = java.lang.Long.bitCount(bits)
}

/**
 * n×n "light toggle": pressing a cell flips it and its orthogonal neighbours; turn every light off.
 *
 * BFS is useless here (2^25 states for 5×5), but presses commute and pressing twice cancels out,
 * so a solution is just a *set* of cells: a linear system over GF(2). [solve] does Gaussian
 * elimination and picks the smallest solution among the null-space combinations.
 */
class LightTogglePuzzle(val size: Int, start: Long) : Puzzle<LightsState, Int> {

    private val cells = size * size

    init {
        require(size in 1..8) { "1..8 supported" }
    }

    override val initialState = LightsState(start)

    /** Bitmask of the cells flipped by pressing [cell]. */
    fun pattern(cell: Int): Long {
        val r = cell / size
        val c = cell % size
        var mask = 1L shl cell
        if (r > 0) mask = mask or (1L shl (cell - size))
        if (r < size - 1) mask = mask or (1L shl (cell + size))
        if (c > 0) mask = mask or (1L shl (cell - 1))
        if (c < size - 1) mask = mask or (1L shl (cell + 1))
        return mask
    }

    override fun legalMoves(state: LightsState): List<Int> = (0 until cells).toList()

    override fun apply(state: LightsState, move: Int) = LightsState(state.bits xor pattern(move))

    override fun status(state: LightsState): PuzzleStatus =
        if (state.bits == 0L) PuzzleStatus.Won else PuzzleStatus.Playing

    override fun describe(move: Int) = "Press the light in row ${move / size + 1}, column ${move % size + 1}"

    override fun solve(from: LightsState): List<Int>? {
        // Equation i: XOR over presses j that flip cell i  ==  whether light i is on now.
        // The press/flip relation is symmetric, so row i's coefficients are just pattern(i).
        val rows = LongArray(cells) { pattern(it) }
        val rhs = BooleanArray(cells) { from.isOn(it) }

        val pivotCols = ArrayList<Int>()
        var rank = 0
        for (col in 0 until cells) {
            val p = (rank until cells).firstOrNull { (rows[it] shr col) and 1L == 1L } ?: continue
            rows[rank] = rows[p].also { rows[p] = rows[rank] }
            rhs[rank] = rhs[p].also { rhs[p] = rhs[rank] }
            for (r in 0 until cells) {
                if (r != rank && (rows[r] shr col) and 1L == 1L) {
                    rows[r] = rows[r] xor rows[rank]
                    rhs[r] = rhs[r] xor rhs[rank]
                }
            }
            pivotCols += col
            rank++
        }
        if ((rank until cells).any { rhs[it] }) return null // unsolvable pattern

        val freeCols = (0 until cells).filter { it !in pivotCols }
        var best: Long? = null
        for (combo in 0L until (1L shl freeCols.size)) {
            var x = 0L
            freeCols.forEachIndexed { k, col -> if ((combo shr k) and 1L == 1L) x = x or (1L shl col) }
            pivotCols.forEachIndexed { r, col ->
                // Row r reads: x[col] XOR (free vars in row r) = rhs[r]
                val freePart = java.lang.Long.bitCount(rows[r] and x and (1L shl col).inv()) % 2 == 1
                if (rhs[r] xor freePart) x = x or (1L shl col)
            }
            if (best == null || java.lang.Long.bitCount(x) < java.lang.Long.bitCount(best)) best = x
        }
        return (0 until cells).filter { (best!! shr it) and 1L == 1L }
    }

    companion object {
        /** Builds a start position by pressing random cells on a dark board, so it's always solvable. */
        fun random(size: Int = 5, presses: Int = 10, random: Random = Random.Default): LightTogglePuzzle {
            val p = LightTogglePuzzle(size, 0L)
            var bits = 0L
            (0 until size * size).shuffled(random).take(presses).forEach { bits = bits xor p.pattern(it) }
            if (bits == 0L) return random(size, presses, random)
            return LightTogglePuzzle(size, bits)
        }
    }
}
