package com.example.sudoku

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sudoku.ui.theme.AppPanel
import com.example.sudoku.ui.theme.SectionEyebrow
import com.example.sudoku.ui.theme.SudokuBackdrop
import com.example.sudoku.ui.theme.SudokuTheme

/**
 * Compose-based launcher that lets the user pick a difficulty before handing off to the
 * ViewBinding-driven MainActivity.
 */
class HomeActivity : ComponentActivity() {

    private var hasSavedGame by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshHomeState()
        setContent {
            SudokuTheme(dynamicColor = false) {
                SudokuBackdrop {
                    DifficultyScreen(
                        hasSavedGame = hasSavedGame,
                        onResumeSavedGame = ::resumeSavedGame,
                        onDifficultySelected = { difficulty -> startGame(difficulty) },
                        onViewStats = ::viewStats,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHomeState()
    }

    private fun startGame(difficulty: SudokuBoard.Difficulty) {
        SavedGameStore.clear(this)
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_DIFFICULTY, difficulty)
        startActivity(intent)
    }

    private fun resumeSavedGame() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_RESUME_SAVED_GAME, true)
        startActivity(intent)
    }

    private fun viewStats() {
        startActivity(Intent(this, StatsActivity::class.java))
    }

    private fun refreshHomeState() {
        hasSavedGame = SavedGameStore.hasSavedGame(this)
    }
}

@Composable
fun DifficultyScreen(
    hasSavedGame: Boolean,
    onResumeSavedGame: () -> Unit,
    onDifficultySelected: (SudokuBoard.Difficulty) -> Unit,
    onViewStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SectionEyebrow(text = stringResource(R.string.home_tagline))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.home_choose_difficulty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(28.dp))
                AppPanel(
                    modifier = Modifier.fillMaxWidth(),
                    emphasized = true
                ) {
                    if (hasSavedGame) {
                        MenuActionButton(
                            title = stringResource(R.string.home_resume_game),
                            subtitle = stringResource(R.string.home_resume_hint),
                            emphasized = true,
                            onClick = onResumeSavedGame
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ) {}
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = stringResource(R.string.home_start_new_game),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    MenuActionButton(
                        title = stringResource(R.string.difficulty_easy),
                        subtitle = stringResource(R.string.difficulty_easy_hint),
                        onClick = { onDifficultySelected(SudokuBoard.Difficulty.EASY) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MenuActionButton(
                        title = stringResource(R.string.difficulty_medium),
                        subtitle = stringResource(R.string.difficulty_medium_hint),
                        onClick = { onDifficultySelected(SudokuBoard.Difficulty.MEDIUM) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MenuActionButton(
                        title = stringResource(R.string.difficulty_hard),
                        subtitle = stringResource(R.string.difficulty_hard_hint),
                        onClick = { onDifficultySelected(SudokuBoard.Difficulty.HARD) }
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    PillActionButton(
                        text = stringResource(R.string.home_view_stats),
                        supportingText = stringResource(R.string.home_stats_hint),
                        onClick = onViewStats,
                        emphasized = false
                    )
                }
            }
        }
    }
}

@Composable
fun MenuActionButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    emphasized: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (emphasized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 7.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (emphasized) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PillActionButton(
    text: String,
    supportingText: String? = null,
    onClick: () -> Unit,
    emphasized: Boolean = false,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasized) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (emphasized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 4.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (supportingText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (emphasized) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DifficultyScreenPreview() {
    SudokuTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SudokuBackdrop {
                DifficultyScreen(
                    hasSavedGame = true,
                    onResumeSavedGame = {},
                    onDifficultySelected = {},
                    onViewStats = {},
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
