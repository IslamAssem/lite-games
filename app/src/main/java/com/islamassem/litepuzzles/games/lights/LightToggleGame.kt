package com.islamassem.litepuzzles.games.lights

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.islamassem.litepuzzles.engine.PuzzleSession
import com.islamassem.litepuzzles.ui.game.PuzzleScreen
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val LightOn = Color(0xFFFFD54F)
private val LightOff = Color(0xFF37474F)
private val PanelColor = Color(0xFF263238)

@Composable
fun LightToggleGame(title: String, rules: String, createPuzzle: () -> LightTogglePuzzle, onExit: () -> Unit) {
    PuzzleScreen(title = title, rules = rules, createPuzzle = createPuzzle, onExit = onExit, randomized = true) { session, modifier ->
        val puzzle = session.puzzle as LightTogglePuzzle
        Column(modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Lights on: ${session.state.litCount}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            LightsBoard(session, puzzle)
            Text(
                "Tapping a light flips it and its neighbours.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun LightsBoard(session: PuzzleSession<LightsState, Int>, puzzle: LightTogglePuzzle) {
    val state = session.state
    val scope = rememberCoroutineScope()
    val n = puzzle.size
    // Press "dip" per cell: the pressed cell sinks, then its neighbours, before the lights flip.
    val dips = remember { List(n * n) { Animatable(1f) } }

    fun press(cell: Int) {
        if (session.isAnimating) return
        scope.launch {
            session.playAnimated(cell) {
                coroutineScope {
                    for (i in 0 until n * n) {
                        if ((puzzle.pattern(cell) shr i) and 1L == 1L) {
                            launch {
                                dips[i].animateTo(0.86f, tween(if (i == cell) 70 else 100))
                                dips[i].animateTo(1f, tween(110))
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(PanelColor)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (r in 0 until n) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (c in 0 until n) {
                    val cell = r * n + c
                    val on = state.isOn(cell)
                    val color by animateColorAsState(if (on) LightOn else LightOff, tween(180), label = "light$cell")
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .graphicsLayer {
                                scaleX = dips[cell].value
                                scaleY = dips[cell].value
                            }
                            .shadow(if (on) 10.dp else 0.dp, RoundedCornerShape(12.dp), ambientColor = LightOn, spotColor = LightOn)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color)
                            .border(2.dp, Color.White.copy(alpha = if (on) 0.6f else 0.08f), RoundedCornerShape(12.dp))
                            .clickable { press(cell) },
                    )
                }
            }
        }
    }
}
