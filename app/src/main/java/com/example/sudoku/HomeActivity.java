package com.example.sudoku;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sudoku.databinding.ActivityHomeBinding;

/**
 * HomeActivity is the entry point of the Sudoku application. It allows users to select the difficulty level of the
 * Sudoku game they want to play.
 */
public class HomeActivity extends AppCompatActivity {

    /**
     * Called when the activity is starting. This is where you should perform one-time initialization such as setting up the
     * UI and listeners.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle
     *        contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHomeBinding binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up listeners for the buttons
        binding.easyButton.setOnClickListener(v -> startGame(SudokuBoard.Difficulty.EASY));
        binding.mediumButton.setOnClickListener(v -> startGame(SudokuBoard.Difficulty.MEDIUM));
        binding.hardButton.setOnClickListener(v -> startGame(SudokuBoard.Difficulty.HARD));
    }

    /**
     * Starts the game with the selected difficulty level.
     *
     * @param difficulty The difficulty level chosen by the user.
     */
    private void startGame(SudokuBoard.Difficulty difficulty) {
        // Create an Intent to launch MainActivity
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);

        // Add the chosen difficulty as an "extra" to the intent.
        // The Difficulty enum implements Serializable, so we can pass it directly.
        intent.putExtra(MainActivity.EXTRA_DIFFICULTY, difficulty);

        startActivity(intent);
    }
}
