package com.example.sudoku

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SudokuTheme(dynamicColor = false) {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DifficultyScreen { difficulty ->
                        startGame(difficulty)
                    }
                }
            }
        }
    }

    private fun startGame(difficulty: SudokuBoard.Difficulty) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_DIFFICULTY, difficulty)
        startActivity(intent)
    }
}

@Composable
fun DifficultyScreen(onDifficultySelected: (SudokuBoard.Difficulty) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sudoku",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = TextBrown,
            modifier = Modifier.padding(bottom = 64.dp)
        )

        DifficultyButton(text = "Facile", onClick = { onDifficultySelected(SudokuBoard.Difficulty.EASY) })
        Spacer(modifier = Modifier.height(16.dp))
        DifficultyButton(text = "Medio", onClick = { onDifficultySelected(SudokuBoard.Difficulty.MEDIUM) })
        Spacer(modifier = Modifier.height(16.dp))
        DifficultyButton(text = "Difficile", onClick = { onDifficultySelected(SudokuBoard.Difficulty.HARD) })
    }
}

@Composable
fun DifficultyButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = ButtonBeige,
            contentColor = TextBrown
        ),
        border = BorderStroke(1.dp, ButtonBorder),
        shape = RoundedCornerShape(8.dp) // Or whatever shape looks best, default is usually rounded
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
fun DifficultyScreenPreview() {
    SudokuTheme(dynamicColor = false) {
        DifficultyScreen { /* Do nothing in preview */ }
    }
}
