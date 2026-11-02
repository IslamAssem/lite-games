package com.islamassem.litepuzzles.engine

import java.util.PriorityQueue

/**
 * A* search: returns a shortest move sequence, exploring far fewer states than BFS when
 * [heuristic] is good. [heuristic] must never overestimate the remaining moves (admissible),
 * otherwise the result may not be optimal. Use it from [Puzzle.solve] overrides.
 */
object AStarSolver {
    fun <S : Any, M : Any> solve(
        puzzle: Puzzle<S, M>,
        from: S,
        heuristic: (S) -> Int,
        maxStates: Int = 1_000_000,
    ): List<M>? {
        when (puzzle.status(from)) {
            PuzzleStatus.Won -> return emptyList()
            is PuzzleStatus.Lost -> return null
            PuzzleStatus.Playing -> Unit
        }
        class Node(val state: S, val cost: Int, val priority: Int)

        val bestCost = HashMap<S, Int>().apply { put(from, 0) }
        val parents = HashMap<S, Pair<S, M>>()
        val open = PriorityQueue<Node>(compareBy<Node> { it.priority }.thenByDescending { it.cost })
        open.add(Node(from, 0, heuristic(from)))

        while (open.isNotEmpty()) {
            val node = open.poll()!!
            if (node.cost > (bestCost[node.state] ?: Int.MAX_VALUE)) continue // stale entry
            when (puzzle.status(node.state)) {
                PuzzleStatus.Won -> return buildPath(parents, node.state)
                is PuzzleStatus.Lost -> continue
                PuzzleStatus.Playing -> Unit
            }
            for (move in puzzle.legalMoves(node.state)) {
                val next = puzzle.apply(node.state, move)
                val cost = node.cost + 1
                if (cost >= (bestCost[next] ?: Int.MAX_VALUE)) continue
                bestCost[next] = cost
                parents[next] = node.state to move
                if (bestCost.size > maxStates) return null
                open.add(Node(next, cost, cost + heuristic(next)))
            }
        }
        return null
    }

    private fun <S : Any, M : Any> buildPath(parents: Map<S, Pair<S, M>>, end: S): List<M> {
        val path = ArrayList<M>()
        var cursor = parents[end]
        while (cursor != null) {
            path.add(cursor.second)
            cursor = parents[cursor.first]
        }
        return path.asReversed()
    }
}
