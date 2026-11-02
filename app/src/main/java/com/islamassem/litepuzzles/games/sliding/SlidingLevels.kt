package com.islamassem.litepuzzles.games.sliding

import com.islamassem.litepuzzles.catalog.Difficulty
import com.islamassem.litepuzzles.catalog.GameCategory
import com.islamassem.litepuzzles.catalog.GameEntry

object SlidingLevels {

    val slidingTiles = GameEntry(
        id = "sliding.tiles_3x3",
        title = "Sliding Tiles",
        description = "Slide the numbered tiles back into order, 1 to 8.",
        emoji = "🔢",
        category = GameCategory.SLIDING,
        difficulty = Difficulty.MEDIUM,
        content = { onExit ->
            SlidingTilesGame(
                title = "Sliding Tiles",
                rules = "Put the tiles in order:\n\n1 2 3\n4 5 6\n7 8 _\n\n" +
                    "• Tap a tile next to the empty space to slide it in.\n" +
                    "• Every game is a new random shuffle, and it can always be solved.",
                createPuzzle = { SlidingTilesPuzzle.scrambled(size = 3) },
                onExit = onExit,
            )
        },
    )
}
