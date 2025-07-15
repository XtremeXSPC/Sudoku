package com.example.sudoku;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sudoku.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {

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

    private void startGame(SudokuBoard.Difficulty difficulty) {
        // Create an Intent to launch MainActivity
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);

        // Add the chosen difficulty as an "extra" to the intent.
        // The Difficulty enum implements Serializable, so we can pass it directly.
        intent.putExtra(MainActivity.EXTRA_DIFFICULTY, difficulty);

        startActivity(intent);
    }
}