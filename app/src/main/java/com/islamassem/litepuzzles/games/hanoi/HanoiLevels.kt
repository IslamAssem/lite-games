package com.islamassem.litepuzzles.games.hanoi

import com.islamassem.litepuzzles.catalog.Difficulty
import com.islamassem.litepuzzles.catalog.GameCategory
import com.islamassem.litepuzzles.catalog.GameEntry

object HanoiLevels {

    val towerOfHanoi = GameEntry(
        id = "sliding.tower_of_hanoi",
        title = "Tower of Hanoi",
        description = "Move the tower of 4 discs to the right peg, one disc at a time.",
        emoji = "🗼",
        category = GameCategory.SLIDING,
        difficulty = Difficulty.MEDIUM,
        content = { onExit ->
            HanoiGame(
                title = "Tower of Hanoi",
                rules = "Move the whole tower from the left peg to the right peg.\n\n" +
                    "• Tap a peg to pick up its top disc, then tap another peg to drop it.\n" +
                    "• Only one disc moves at a time.\n" +
                    "• A disc can never sit on top of a smaller disc.\n\n" +
                    "The best possible solution takes 15 moves.",
                createPuzzle = { HanoiPuzzle(discCount = 4) },
                onExit = onExit,
            )
        },
    )
}
