package com.islamassem.litepuzzles.games.rivercrossing

import com.islamassem.litepuzzles.engine.Puzzle
import com.islamassem.litepuzzles.engine.PuzzleStatus

/** [kind] groups interchangeable passengers (e.g. three "missionary"s) for counting rules. */
data class Passenger(
    val id: String,
    val name: String,
    val emoji: String,
    val kind: String = id,
    val canRow: Boolean = true,
)

enum class Bank {
    START, END;

    val other: Bank get() = if (this == START) END else START
}

data class RiverState(val positions: Map<String, Bank>, val boat: Bank) {
    fun on(bank: Bank): Set<String> = positions.filterValues { it == bank }.keys
}

data class Crossing(val passengerIds: Set<String>)

/**
 * Checks one bank after a crossing (passengers have stepped off the boat).
 * Returns a loss reason, or null if the bank is safe.
 */
fun interface BankRule {
    fun violation(onBank: List<Passenger>): String?
}

/**
 * Generic river-crossing puzzle: move everyone from START to END. The boat carries
 * 1..[boatCapacity] passengers, at least one of whom can row. After each crossing every
 * [BankRule] is checked on both banks.
 */
class RiverCrossingPuzzle(
    val passengers: List<Passenger>,
    val boatCapacity: Int,
    private val rules: List<BankRule>,
) : Puzzle<RiverState, Crossing> {

    private val byId = passengers.associateBy { it.id }

    init {
        require(byId.size == passengers.size) { "Passenger ids must be unique" }
        require(passengers.any { it.canRow }) { "Someone must be able to row" }
    }

    fun passenger(id: String): Passenger = byId.getValue(id)

    override val initialState = RiverState(passengers.associate { it.id to Bank.START }, Bank.START)

    override fun legalMoves(state: RiverState): List<Crossing> {
        val available = passengers.filter { state.positions[it.id] == state.boat }
        return subsets(available, boatCapacity)
            .filter { group -> group.any { it.canRow } }
            .map { group -> Crossing(group.mapTo(LinkedHashSet()) { it.id }) }
    }

    fun isLegal(state: RiverState, move: Crossing): Boolean {
        val group = move.passengerIds.map { byId[it] ?: return false }
        return group.size in 1..boatCapacity &&
            group.any { it.canRow } &&
            group.all { state.positions[it.id] == state.boat }
    }

    override fun apply(state: RiverState, move: Crossing): RiverState {
        require(isLegal(state, move)) { "Illegal crossing $move" }
        val to = state.boat.other
        return RiverState(
            positions = state.positions + move.passengerIds.associateWith { to },
            boat = to,
        )
    }

    override fun status(state: RiverState): PuzzleStatus {
        for (bank in Bank.entries) {
            val here = state.on(bank).map(byId::getValue)
            for (rule in rules) rule.violation(here)?.let { return PuzzleStatus.Lost(it) }
        }
        return if (state.on(Bank.START).isEmpty()) PuzzleStatus.Won else PuzzleStatus.Playing
    }

    override fun describe(move: Crossing): String {
        val names = move.passengerIds.map { passenger(it).name }
        return when (names.size) {
            1 -> "${names[0]} crosses alone"
            else -> names.dropLast(1).joinToString(", ") + " and ${names.last()} cross together"
        }
    }

    companion object {
        /** [a] and [b] can't share a bank unless [guard] is there. */
        fun notTogetherWithout(a: String, b: String, guard: String, reason: String) = BankRule { bank ->
            val ids = bank.map { it.id }
            if (a in ids && b in ids && guard !in ids) reason else null
        }

        /** On any bank where some [victimKind] is present, [threatKind] must not outnumber it. */
        fun mustNotOutnumber(threatKind: String, victimKind: String, reason: String) = BankRule { bank ->
            val threats = bank.count { it.kind == threatKind }
            val victims = bank.count { it.kind == victimKind }
            if (victims > 0 && threats > victims) reason else null
        }

        private fun <T> subsets(items: List<T>, maxSize: Int): List<List<T>> {
            val result = mutableListOf<List<T>>()
            fun build(start: Int, current: List<T>) {
                if (current.isNotEmpty()) result += current
                if (current.size == maxSize) return
                for (i in start until items.size) build(i + 1, current + items[i])
            }
            build(0, emptyList())
            return result
        }
    }
}
