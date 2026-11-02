package com.islamassem.litepuzzles.catalog

import com.islamassem.litepuzzles.games.hanoi.HanoiLevels
import com.islamassem.litepuzzles.games.lights.LightsLevels
import com.islamassem.litepuzzles.games.sliding.SlidingLevels
import com.islamassem.litepuzzles.games.pouring.PouringLevels
import com.islamassem.litepuzzles.games.rivercrossing.RiverCrossingLevels

/**
 * The single list of games shown on the home screen.
 * To add a game: create it under `games/<name>/`, expose a [GameEntry], and add it here.
 */
object GameRegistry {
    val games: List<GameEntry> = listOf(
        RiverCrossingLevels.farmerWolfGoatCabbage,
        RiverCrossingLevels.missionariesAndCannibals,
        PouringLevels.waterJugs,
        HanoiLevels.towerOfHanoi,
        SlidingLevels.slidingTiles,
        LightsLevels.lightToggle,
    )

    init {
        val duplicates = games.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate game ids: $duplicates" }
    }

    fun find(id: String): GameEntry? = games.firstOrNull { it.id == id }
}
