package com.example.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sudoku.ui.theme.AppPanel
import com.example.sudoku.ui.theme.SectionEyebrow
import com.example.sudoku.ui.theme.SudokuBackdrop
import com.example.sudoku.ui.theme.SudokuTheme

/**
 * Compose activity that shows aggregated local player statistics.
 */
class StatsActivity : ComponentActivity() {

    private var statsSnapshot by mutableStateOf(GameStatsStore.emptySnapshot())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshStats()
        setContent {
            SudokuTheme(dynamicColor = false) {
                SudokuBackdrop {
                    StatsScreen(
                        statsSnapshot = statsSnapshot,
                        onBack = ::finish,
                        modifier = Modifier.align(Alignment.Center)
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

/**
 * Stats dashboard with global overview and per-difficulty breakdown.
 */
@Composable
fun StatsScreen(
    statsSnapshot: GameStatsStore.StatsSnapshot,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unavailableLabel = stringResource(R.string.stats_not_available)
    val overview = buildStatsOverview(statsSnapshot)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                SectionEyebrow(text = stringResource(R.string.stats_title))
                Text(
                    text = stringResource(R.string.stats_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.stats_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!statsSnapshot.hasAnyStats()) {
                    AppPanel(modifier = Modifier.fillMaxWidth(), emphasized = true) {
                        Text(
                            text = stringResource(R.string.stats_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start
                        )
                    }
                } else {
                    AppPanel(modifier = Modifier.fillMaxWidth(), emphasized = true) {
                        Text(
                            text = stringResource(R.string.stats_overview_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        StatsMetricRow(
                            label = stringResource(R.string.stats_total_completed_label),
                            value = pluralStringResource(R.plurals.stats_wins, overview.totalWins, overview.totalWins),
                            isUnavailable = false
                        )
                        StatsMetricRow(
                            label = stringResource(R.string.stats_fastest_completion_label),
                            value = formatBestTime(overview.fastestTimeInMillis, unavailableLabel),
                            isUnavailable = overview.fastestTimeInMillis == null || overview.fastestTimeInMillis < 0
                        )
                        StatsMetricRow(
                            label = stringResource(R.string.stats_best_score_label),
                            value = overview.bestScore?.toString() ?: unavailableLabel,
                            isUnavailable = overview.bestScore == null
                        )
                    }

                    Text(
                        text = stringResource(R.string.stats_by_difficulty_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

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

                Spacer(modifier = Modifier.height(4.dp))
                PillActionButton(
                    text = stringResource(R.string.back_to_home_button),
                    onClick = onBack,
                    emphasized = true
                )
            }
        }
    }
}

@Composable
private fun DifficultyStatsCard(
    label: String,
    stats: GameStatsStore.DifficultyStats,
    unavailableLabel: String
) {
    AppPanel(modifier = Modifier.fillMaxWidth()) {
        SectionEyebrow(text = label)
        Spacer(modifier = Modifier.height(14.dp))
        StatsMetricRow(
            label = stringResource(R.string.stats_difficulty_wins_label),
            value = pluralStringResource(R.plurals.stats_wins, stats.wins, stats.wins),
            isUnavailable = false
        )
        StatsMetricRow(
            label = stringResource(R.string.stats_difficulty_best_time_label),
            value = formatBestTime(stats.bestTimeInMillis, unavailableLabel),
            isUnavailable = stats.bestTimeInMillis == null || stats.bestTimeInMillis < 0
        )
        StatsMetricRow(
            label = stringResource(R.string.stats_difficulty_best_score_label),
            value = if (stats.bestScore >= 0) stats.bestScore.toString() else unavailableLabel,
            isUnavailable = stats.bestScore < 0
        )
    }
}

@Composable
private fun StatsMetricRow(label: String, value: String, isUnavailable: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isUnavailable || value == stringResource(R.string.stats_not_available)) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
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

/**
 * Aggregates cross-difficulty metrics used by the overview card.
 */
private fun buildStatsOverview(statsSnapshot: GameStatsStore.StatsSnapshot): StatsOverview {
    val statsByDifficulty = listOf(
        statsSnapshot.getStats(SudokuBoard.Difficulty.EASY),
        statsSnapshot.getStats(SudokuBoard.Difficulty.MEDIUM),
        statsSnapshot.getStats(SudokuBoard.Difficulty.HARD)
    )

    var bestScoreTotal: Int? = null
    var fastestTimeTotal: Long? = null
    var totalWins = 0

    for (stat in statsByDifficulty) {
        totalWins += stat.wins
        
        if (stat.bestScore >= 0) {
            if (bestScoreTotal == null || stat.bestScore > bestScoreTotal) {
                bestScoreTotal = stat.bestScore
            }
        }
        
        if (stat.bestTimeInMillis >= 0) {
            if (fastestTimeTotal == null || stat.bestTimeInMillis < fastestTimeTotal) {
                fastestTimeTotal = stat.bestTimeInMillis
            }
        }
    }

    return StatsOverview(
        totalWins = totalWins,
        bestScore = bestScoreTotal,
        fastestTimeInMillis = fastestTimeTotal
    )
}

private data class StatsOverview(
    val totalWins: Int,
    val bestScore: Int?,
    val fastestTimeInMillis: Long?
)
