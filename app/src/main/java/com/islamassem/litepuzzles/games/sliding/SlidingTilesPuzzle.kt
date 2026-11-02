package com.islamassem.litepuzzles.games.sliding

import com.islamassem.litepuzzles.engine.AStarSolver
import com.islamassem.litepuzzles.engine.Puzzle
import com.islamassem.litepuzzles.engine.PuzzleStatus
import kotlin.math.abs
import kotlin.random.Random

/** Row-major tile numbers; 0 is the empty space. */
data class TilesState(val tiles: List<Int>) {
    val blank: Int get() = tiles.indexOf(0)
}

enum class Direction { UP, DOWN, LEFT, RIGHT }

/** Slide tile number [tile] into the empty space, moving it in [direction]. */
data class SlideMove(val tile: Int, val direction: Direction)

/**
 * The n×n sliding-tile puzzle (8-puzzle for n = 3): put the tiles back in order with the gap
 * in the bottom-right corner. Solved optimally with A* + Manhattan distance, which is instant
 * for 3×3; 4×4 would need a stronger heuristic.
 */
class SlidingTilesPuzzle(val size: Int, start: List<Int>) : Puzzle<TilesState, SlideMove> {

    private val solved = (1 until size * size) + 0

    init {
        require(start.sorted() == solved.sorted()) { "Start must be a permutation of 0..${size * size - 1}" }
    }

    override val initialState = TilesState(start)

    fun row(index: Int) = index / size
    fun col(index: Int) = index % size

    /** Tiles next to the gap, each with the direction it would slide (towards the gap). */
    override fun legalMoves(state: TilesState): List<SlideMove> {
        val b = state.blank
        val n = size // not inside buildList: there `size` would mean the list's size
        val moves = ArrayList<SlideMove>(4)
        if (row(b) > 0) moves += SlideMove(state.tiles[b - n], Direction.DOWN)
        if (row(b) < n - 1) moves += SlideMove(state.tiles[b + n], Direction.UP)
        if (col(b) > 0) moves += SlideMove(state.tiles[b - 1], Direction.RIGHT)
        if (col(b) < n - 1) moves += SlideMove(state.tiles[b + 1], Direction.LEFT)
        return moves
    }

    /** The move that slides [tile], or null if it isn't next to the gap. */
    fun moveFor(state: TilesState, tile: Int): SlideMove? = legalMoves(state).firstOrNull { it.tile == tile }

    override fun apply(state: TilesState, move: SlideMove): TilesState {
        require(move in legalMoves(state)) { "Tile ${move.tile} can't slide ${move.direction}" }
        val tiles = state.tiles.toMutableList()
        val from = tiles.indexOf(move.tile)
        tiles[state.blank] = move.tile
        tiles[from] = 0
        return TilesState(tiles)
    }

    override fun status(state: TilesState): PuzzleStatus =
        if (state.tiles == solved) PuzzleStatus.Won else PuzzleStatus.Playing

    override fun describe(move: SlideMove) = "Slide the ${move.tile} ${move.direction.name.lowercase()}"

    override fun solve(from: TilesState): List<SlideMove>? = AStarSolver.solve(this, from, ::manhattan)

    /** Sum over tiles of their grid distance from home: never overestimates the moves left. */
    fun manhattan(state: TilesState): Int {
        var total = 0
        state.tiles.forEachIndexed { index, tile ->
            if (tile != 0) {
                val home = tile - 1
                total += abs(row(index) - row(home)) + abs(col(index) - col(home))
            }
        }
        return total
    }

    companion object {
        /** Scrambles by random legal slides from the solved board, so it is always solvable. */
        fun scrambled(size: Int = 3, random: Random = Random.Default, slides: Int = 150): SlidingTilesPuzzle {
            val solvedPuzzle = SlidingTilesPuzzle(size, (1 until size * size) + 0)
            var state = solvedPuzzle.initialState
            var previous: Int? = null
            repeat(slides) {
                val options = solvedPuzzle.legalMoves(state).filter { it.tile != previous }
                val move = options.random(random)
                state = solvedPuzzle.apply(state, move)
                previous = move.tile
            }
            // A barely-scrambled board is no fun: retry if it ended up almost solved.
            if (solvedPuzzle.manhattan(state) < 10) return scrambled(size, random, slides)
            return SlidingTilesPuzzle(size, state.tiles)
        }
    }
}
