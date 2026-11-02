package com.islamassem.litepuzzles.engine

/** Breadth-first search: returns a shortest move sequence to a won state. */
object BfsSolver {
    const val DEFAULT_MAX_STATES = 200_000

    fun <S : Any, M : Any> solve(
        puzzle: Puzzle<S, M>,
        from: S = puzzle.initialState,
        maxStates: Int = DEFAULT_MAX_STATES,
    ): List<M>? {
        when (puzzle.status(from)) {
            PuzzleStatus.Won -> return emptyList()
            is PuzzleStatus.Lost -> return null
            PuzzleStatus.Playing -> Unit
        }
        // Each visited state maps to (parent state, move taken from parent).
        val parents = HashMap<S, Pair<S, M>?>()
        parents[from] = null
        val queue = ArrayDeque<S>().apply { add(from) }

        while (queue.isNotEmpty()) {
            val state = queue.removeFirst()
            for (move in puzzle.legalMoves(state)) {
                val next = puzzle.apply(state, move)
                if (next in parents) continue
                parents[next] = state to move
                when (puzzle.status(next)) {
                    PuzzleStatus.Won -> return buildPath(parents, next)
                    is PuzzleStatus.Lost -> Unit
                    PuzzleStatus.Playing -> {
                        if (parents.size >= maxStates) return null
                        queue.addLast(next)
                    }
                }
            }
        }
        return null
    }

    private fun <S : Any, M : Any> buildPath(parents: Map<S, Pair<S, M>?>, end: S): List<M> {
        val path = ArrayList<M>()
        var cursor = parents[end]
        while (cursor != null) {
            path.add(cursor.second)
            cursor = parents[cursor.first]
        }
        return path.asReversed()
    }
}
