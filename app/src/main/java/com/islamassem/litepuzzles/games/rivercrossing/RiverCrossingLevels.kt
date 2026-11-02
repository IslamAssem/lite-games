package com.islamassem.litepuzzles.games.rivercrossing

import com.islamassem.litepuzzles.catalog.Difficulty
import com.islamassem.litepuzzles.catalog.GameCategory
import com.islamassem.litepuzzles.catalog.GameEntry
import com.islamassem.litepuzzles.games.rivercrossing.RiverCrossingPuzzle.Companion.mustNotOutnumber
import com.islamassem.litepuzzles.games.rivercrossing.RiverCrossingPuzzle.Companion.notTogetherWithout

/** River-crossing levels. Each one is just passengers + boat size + rules. */
object RiverCrossingLevels {

    fun farmerWolfGoatCabbagePuzzle() = RiverCrossingPuzzle(
        passengers = listOf(
            Passenger("farmer", "Farmer", "👨‍🌾"),
            Passenger("wolf", "Wolf", "🐺", canRow = false),
            Passenger("goat", "Goat", "🐐", canRow = false),
            Passenger("cabbage", "Cabbage", "🥬", canRow = false),
        ),
        boatCapacity = 2,
        rules = listOf(
            notTogetherWithout("wolf", "goat", guard = "farmer", reason = "The wolf ate the goat!"),
            notTogetherWithout("goat", "cabbage", guard = "farmer", reason = "The goat ate the cabbage!"),
        ),
    )

    fun missionariesAndCannibalsPuzzle() = RiverCrossingPuzzle(
        passengers = (1..3).map { Passenger("m$it", "Missionary $it", "🧑‍🏫", kind = "missionary") } +
            (1..3).map { Passenger("c$it", "Cannibal $it", "👹", kind = "cannibal") },
        boatCapacity = 2,
        rules = listOf(
            mustNotOutnumber("cannibal", "missionary", reason = "The cannibals outnumbered the missionaries!"),
        ),
    )

    val farmerWolfGoatCabbage = GameEntry(
        id = "river.farmer_wolf_goat_cabbage",
        title = "Farmer, Wolf, Goat & Cabbage",
        description = "Ferry everything across without anything getting eaten.",
        emoji = "🐐",
        category = GameCategory.RIVER_CROSSING,
        difficulty = Difficulty.EASY,
        content = { onExit ->
            RiverCrossingGame(
                title = "Farmer, Wolf, Goat & Cabbage",
                rules = "Get everyone to the other bank.\n\n" +
                    "• Only the farmer can row.\n" +
                    "• The boat holds the farmer and one more.\n" +
                    "• Left without the farmer, the wolf eats the goat, and the goat eats the cabbage.",
                createPuzzle = ::farmerWolfGoatCabbagePuzzle,
                onExit = onExit,
            )
        },
    )

    val missionariesAndCannibals = GameEntry(
        id = "river.missionaries_cannibals",
        title = "Missionaries & Cannibals",
        description = "Three of each, one small boat. Never let the missionaries be outnumbered.",
        emoji = "🛶",
        category = GameCategory.RIVER_CROSSING,
        difficulty = Difficulty.MEDIUM,
        content = { onExit ->
            RiverCrossingGame(
                title = "Missionaries & Cannibals",
                rules = "Get all six people to the other bank.\n\n" +
                    "• The boat holds one or two people, and anyone can row.\n" +
                    "• If cannibals ever outnumber missionaries on a bank, you lose.\n" +
                    "• People in the boat count as being on the bank they arrive at.",
                createPuzzle = ::missionariesAndCannibalsPuzzle,
                onExit = onExit,
            )
        },
    )
}
