package com.islamassem.litepuzzles.games.lights

import com.islamassem.litepuzzles.catalog.Difficulty
import com.islamassem.litepuzzles.catalog.GameCategory
import com.islamassem.litepuzzles.catalog.GameEntry

object LightsLevels {

    val lightToggle = GameEntry(
        id = "grid.light_toggle_5x5",
        title = "Light Toggle",
        description = "Every tap flips a light and its neighbours. Switch them all off.",
        emoji = "💡",
        category = GameCategory.GRID_LOGIC,
        difficulty = Difficulty.MEDIUM,
        content = { onExit ->
            LightToggleGame(
                title = "Light Toggle",
                rules = "Turn every light off.\n\n" +
                    "• Tapping a light flips it and the lights directly above, below, left and right of it.\n" +
                    "• Tapping the same light twice undoes it, so the order doesn't matter.\n" +
                    "• Every game is a new random board, and it can always be solved.",
                createPuzzle = { LightTogglePuzzle.random(size = 5, presses = 10) },
                onExit = onExit,
            )
        },
    )
}
