package com.example.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sudoku.ui.theme.ButtonBeige
import com.example.sudoku.ui.theme.ButtonBorder
import com.example.sudoku.ui.theme.SudokuTheme
import com.example.sudoku.ui.theme.TextBrown

class StatsActivity : ComponentActivity() {

    private var statsSnapshot by mutableStateOf(GameStatsStore.emptySnapshot())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshStats()
        setContent {
            SudokuTheme(dynamicColor = false) {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    StatsScreen(
                            statsSnapshot = statsSnapshot,
                            onBack = ::finish
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }

    private fun refreshStats() {
        statsSnapshot = GameStatsStore.load(this)
    }
}

@Composable
fun StatsScreen(
        statsSnapshot: GameStatsStore.StatsSnapshot,
        onBack: () -> Unit
) {
    val unavailableLabel = stringResource(R.string.stats_not_available)
    val overview = buildStatsOverview(statsSnapshot)

    Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
                text = stringResource(R.string.stats_title),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TextBrown
        )
        Text(
                text = stringResource(R.string.stats_subtitle),
                fontSize = 16.sp,
                color = TextBrown
        )

        if (!statsSnapshot.hasAnyStats()) {
            StatsCard {
                Text(
                        text = stringResource(R.string.stats_empty),
                        fontSize = 15.sp,
                        color = TextBrown
                )
            }
        } else {
            StatsSectionTitle(text = stringResource(R.string.stats_overview_title))
            StatsCard {
                StatsValueRow(
                        label = stringResource(R.string.stats_total_completed_label),
                        value = pluralStringResource(R.plurals.stats_wins, overview.totalWins, overview.totalWins)
                )
                StatsValueRow(
                        label = stringResource(R.string.stats_fastest_completion_label),
                        value = formatBestTime(overview.fastestTimeInMillis, unavailableLabel)
                )
                StatsValueRow(
                        label = stringResource(R.string.stats_best_score_label),
                        value = overview.bestScore?.toString() ?: unavailableLabel
                )
            }

            StatsSectionTitle(text = stringResource(R.string.stats_by_difficulty_title))
            DifficultyStatsCard(
                    label = stringResource(R.string.difficulty_easy),
                    stats = statsSnapshot.getStats(SudokuBoard.Difficulty.EASY),
                    unavailableLabel = unavailableLabel
            )
            DifficultyStatsCard(
                    label = stringResource(R.string.difficulty_medium),
                    stats = statsSnapshot.getStats(SudokuBoard.Difficulty.MEDIUM),
                    unavailableLabel = unavailableLabel
            )
            DifficultyStatsCard(
                    label = stringResource(R.string.difficulty_hard),
                    stats = statsSnapshot.getStats(SudokuBoard.Difficulty.HARD),
                    unavailableLabel = unavailableLabel
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        PrimaryActionButton(
                text = stringResource(R.string.back_to_home_button),
                onClick = onBack
        )
    }
}

@Composable
private fun StatsSectionTitle(text: String) {
    Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextBrown
    )
}

@Composable
private fun StatsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = ButtonBeige.copy(alpha = 0.28f),
            border = BorderStroke(1.dp, ButtonBorder)
    ) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
        )
    }
}

@Composable
private fun DifficultyStatsCard(
        label: String,
        stats: GameStatsStore.DifficultyStats,
        unavailableLabel: String
) {
    StatsCard {
        Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextBrown
        )
        StatsValueRow(
                label = stringResource(R.string.stats_difficulty_wins_label),
                value = pluralStringResource(R.plurals.stats_wins, stats.wins, stats.wins)
        )
        StatsValueRow(
                label = stringResource(R.string.stats_difficulty_best_time_label),
                value = formatBestTime(stats.bestTimeInMillis, unavailableLabel)
        )
        StatsValueRow(
                label = stringResource(R.string.stats_difficulty_best_score_label),
                value = if (stats.bestScore >= 0) stats.bestScore.toString() else unavailableLabel
        )
    }
}

@Composable
private fun StatsValueRow(label: String, value: String) {
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
                text = label,
                fontSize = 14.sp,
                color = TextBrown,
                modifier = Modifier.padding(end = 16.dp)
        )
        Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextBrown
        )
    }
}

private fun formatBestTime(bestTimeInMillis: Long?, unavailableLabel: String): String {
    if (bestTimeInMillis == null || bestTimeInMillis < 0) {
        return unavailableLabel
    }

    val totalSeconds = bestTimeInMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun buildStatsOverview(statsSnapshot: GameStatsStore.StatsSnapshot): StatsOverview {
    val statsByDifficulty = listOf(
            statsSnapshot.getStats(SudokuBoard.Difficulty.EASY),
            statsSnapshot.getStats(SudokuBoard.Difficulty.MEDIUM),
            statsSnapshot.getStats(SudokuBoard.Difficulty.HARD)
    )

    val bestScore = statsByDifficulty
            .map { it.bestScore }
            .filter { it >= 0 }
            .maxOrNull()
    val fastestTime = statsByDifficulty
            .map { it.bestTimeInMillis }
            .filter { it >= 0 }
            .minOrNull()

    return StatsOverview(
            totalWins = statsByDifficulty.sumOf { it.wins },
            bestScore = bestScore,
            fastestTimeInMillis = fastestTime
    )
}

private data class StatsOverview(
        val totalWins: Int,
        val bestScore: Int?,
        val fastestTimeInMillis: Long?
)
