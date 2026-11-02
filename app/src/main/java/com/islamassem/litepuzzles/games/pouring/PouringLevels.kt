package com.islamassem.litepuzzles.games.pouring

import com.islamassem.litepuzzles.catalog.Difficulty
import com.islamassem.litepuzzles.catalog.GameCategory
import com.islamassem.litepuzzles.catalog.GameEntry

/** Pouring levels. Each one is just jug sizes, start amounts, tap/drain on or off, and a goal. */
object PouringLevels {

    fun waterJugsPuzzle() = WaterJugsPuzzle(
        capacities = listOf(3, 5),
        tapAndDrain = true,
        goalText = "Measure exactly 4 L",
        isGoal = { amounts -> 4 in amounts },
    )

    val waterJugs = GameEntry(
        id = "pouring.water_jugs",
        title = "Water Jugs",
        description = "A 3 L jug, a 5 L jug and a tap. Measure exactly 4 litres.",
        emoji = "🫗",
        category = GameCategory.POURING,
        difficulty = Difficulty.EASY,
        content = { onExit ->
            WaterJugsGame(
                title = "Water Jugs",
                rules = "Get exactly 4 litres in one of the jugs.\n\n" +
                    "• Fill a jug to the top from the tap, or empty it out.\n" +
                    "• Tap a jug to pick it up, then tap the other jug to pour into it.\n" +
                    "• Pouring stops when the first jug is empty or the second is full.",
                createPuzzle = ::waterJugsPuzzle,
                onExit = onExit,
            )
        },
    )
}
