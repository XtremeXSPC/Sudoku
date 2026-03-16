package com.example.sudoku

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sudoku.ui.theme.ButtonBeige
import com.example.sudoku.ui.theme.ButtonBorder
import com.example.sudoku.ui.theme.SudokuTheme
import com.example.sudoku.ui.theme.TextBrown

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
                // A surface container using the 'background' color from the theme
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    DifficultyScreen(
                            hasSavedGame = hasSavedGame,
                            onResumeSavedGame = ::resumeSavedGame,
                            onDifficultySelected = { difficulty -> startGame(difficulty) },
                            onViewStats = ::viewStats
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
        onViewStats: () -> Unit
) {
    Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
                text = stringResource(R.string.app_name),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = TextBrown,
                modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = stringResource(R.string.home_choose_difficulty),
            fontSize = 18.sp,
            color = TextBrown,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        if (hasSavedGame) {
            PrimaryActionButton(
                    text = stringResource(R.string.home_resume_game),
                    onClick = onResumeSavedGame
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                    text = stringResource(R.string.home_start_new_game),
                    fontSize = 15.sp,
                    color = TextBrown,
                    modifier = Modifier.padding(bottom = 20.dp)
            )
        }

        PrimaryActionButton(
                text = stringResource(R.string.difficulty_easy),
                onClick = { onDifficultySelected(SudokuBoard.Difficulty.EASY) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryActionButton(
                text = stringResource(R.string.difficulty_medium),
                onClick = { onDifficultySelected(SudokuBoard.Difficulty.MEDIUM) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryActionButton(
                text = stringResource(R.string.difficulty_hard),
                onClick = { onDifficultySelected(SudokuBoard.Difficulty.HARD) }
        )

        Spacer(modifier = Modifier.height(32.dp))
        PrimaryActionButton(
                text = stringResource(R.string.home_view_stats),
                onClick = onViewStats
        )
    }
}

@Composable
fun PrimaryActionButton(text: String, onClick: () -> Unit) {
    Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors =
                    ButtonDefaults.buttonColors(
                            containerColor = ButtonBeige,
                            contentColor = TextBrown
                    ),
            border = BorderStroke(1.dp, ButtonBorder),
            shape =
                    RoundedCornerShape(
                            8.dp
                    ) // Or whatever shape looks best, default is usually rounded
    ) { Text(text = text) }
}

@Preview(showBackground = true)
@Composable
fun DifficultyScreenPreview() {
    SudokuTheme(dynamicColor = false) {
        DifficultyScreen(
                hasSavedGame = true,
                onResumeSavedGame = {},
                onDifficultySelected = {},
                onViewStats = {}
        )
    }
}
