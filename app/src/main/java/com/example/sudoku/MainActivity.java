package com.example.sudoku;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;

import com.example.sudoku.databinding.ActivityMainBinding;
import com.example.sudoku.viewmodel.SudokuViewModel;

import java.util.Locale;
import java.util.Objects;

/**
 * The main activity for the Sudoku game. This class is responsible for setting up the UI, observing the game state from
 * the SudokuViewModel, and handling user interactions like button presses and cell selections.
 */
public class MainActivity extends AppCompatActivity {

    // Intent keys
    public static final String EXTRA_DIFFICULTY = "com.example.sudoku.DIFFICULTY";
    // Bundle keys
    private static final String KEY_SUDOKU_BOARD_STATE = "sudokuBoardState";
    private static final String KEY_VIEW_MODEL_BUNDLE_STATE = "viewModelBundleState";

    // ViewBinding and ViewModel declaration.
    private ActivityMainBinding binding;
    private SudokuViewModel viewModel;
    private HighlightOverlayView highlightOverlayView;

    // Array to hold the grid TextViews for quick access.
    private final TextView[][] cellTextViews = new TextView[9][9];

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout using ViewBinding.
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get the ViewModel instance using ViewModelProvider.
        viewModel = new ViewModelProvider(this).get(SudokuViewModel.class);

        // Initialize the HighlightOverlayView.
        highlightOverlayView = findViewById(R.id.highlightOverlayView);

        // State restoration logic.
        if (savedInstanceState == null) {
            // This is the first boot, not a rotation. Let's read the intent.
            SudokuBoard.Difficulty difficulty;
            // Handle the modern and deprecated getParcelable methods based on API level.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                difficulty = getIntent().getSerializableExtra(EXTRA_DIFFICULTY,
                        SudokuBoard.Difficulty.class);
            } else {
                // Suppress the deprecation warning for older APIs.
                difficulty =
                        (SudokuBoard.Difficulty) getIntent().getSerializableExtra(EXTRA_DIFFICULTY);
            }

            // If for some reason the difficulty is null, we use a default.
            if (difficulty == null) {
                difficulty = SudokuBoard.Difficulty.MEDIUM;
            }

            // We explicitly tell the ViewModel to start a new game.
            viewModel.startNewGame(difficulty);

        } else {
            // Restore state after process death. The ViewModel is newly created, so we need to
            // restore its state.
            SudokuBoard boardState;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                boardState =
                        savedInstanceState.getParcelable(KEY_SUDOKU_BOARD_STATE, SudokuBoard.class);
            } else {
                boardState = savedInstanceState.getParcelable(KEY_SUDOKU_BOARD_STATE);
            }
            Bundle viewModelBundle = savedInstanceState.getBundle(KEY_VIEW_MODEL_BUNDLE_STATE);

            if (boardState != null && viewModelBundle != null) {
                viewModel.restoreState(boardState, viewModelBundle);
            } else {
                // Fallback: if state is missing, start a new game.
                SudokuBoard.Difficulty difficulty;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    difficulty = getIntent().getSerializableExtra(EXTRA_DIFFICULTY,
                            SudokuBoard.Difficulty.class);
                } else {
                    difficulty = (SudokuBoard.Difficulty) getIntent()
                            .getSerializableExtra(EXTRA_DIFFICULTY);
                }
                if (difficulty == null) {
                    difficulty = SudokuBoard.Difficulty.MEDIUM;
                }
                viewModel.startNewGame(difficulty);
            }
        }

        setupNumberPad();
        setupActionButtons();
        observeViewModel();

        // Use a ViewTreeObserver to create the grid TextViews once the container's size is known.
        // In Java, we use an anonymous inner class for the listener.
        binding.sudokuContainer.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // It's crucial to remove the listener to prevent it from being called
                        // multiple times.
                        binding.sudokuContainer.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);

                        initializeSudokuGridOverlay(binding.sudokuContainer.getWidth() / 9);

                        // Force an initial UI update based on ViewModel data, especially useful
                        // after a rotation.
                        if (viewModel.getSudokuBoard().getValue() != null
                                && cellTextViews[0][0] != null) {
                            updateGridUI(viewModel.getSudokuBoard().getValue());
                        }
                        if (viewModel.getSelectedCell().getValue() != null
                                && cellTextViews[0][0] != null) {
                            Pair<Integer, Integer> selection = viewModel.getSelectedCell().getValue();
                            // updateSelectedCellUI(selection.first, selection.second);
                        }
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Get the state from the ViewModel.
        Pair<SudokuBoard, Bundle> state = viewModel.saveState();
        if (state.first != null) {
            outState.putParcelable(KEY_SUDOKU_BOARD_STATE, state.first);
        }
        if (state.second != null) {
            outState.putBundle(KEY_VIEW_MODEL_BUNDLE_STATE, state.second);
        }
    }

    /**
     * Maps a Difficulty enum to its corresponding user-friendly string resource ID.
     * 
     * @param difficulty The difficulty level from the enum.
     * @return The integer resource ID (e.g., R.string.difficulty_easy) for the difficulty string.
     */
    private int getDifficultyStringRes(SudokuBoard.Difficulty difficulty) {
        if (difficulty == null) {
            // Fallback in case difficulty is null
            return R.string.difficulty_medium;
        }
        return switch (difficulty) {
            case EASY -> R.string.difficulty_easy;
            case HARD -> R.string.difficulty_hard;
            default -> R.string.difficulty_medium;
        };
    }

    /**
     * Initializes the number pad (buttons 1-9).
     */
    private void setupNumberPad() {
        for (int i = 1; i <= 9; i++) {
            // The lambda onClickListener needs to capture the value of 'i'.
            final int numberToInput = i;

            Button button = new Button(this);
            button.setText(String.valueOf(i));
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            button.setOnClickListener(v -> viewModel.inputNumber(numberToInput));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0; // Set width to 0 to use weight
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // Distribute equally
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(4, 4, 4, 4);
            button.setLayoutParams(params);

            button.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.button_beige));
            button.setTextColor(ContextCompat.getColor(this, R.color.text_brown));

            binding.numberPad.addView(button);
        }
    }

    /**
     * Initializes the action buttons (Undo, New Game).
     */
    private void setupActionButtons() {
        binding.undoButton.setOnClickListener(v -> {
            if (!viewModel.undoLastMove()) {
                Toast.makeText(this, getString(R.string.no_moves_to_undo), Toast.LENGTH_SHORT)
                        .show();
            }
        });
        binding.newGameButton.setOnClickListener(v -> returnToHome());
    }

    /**
     * Ends the current game session and returns to the HomeActivity to start a new one.
     */
    private void returnToHome() {
        // Create an Intent to return to HomeActivity.
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);

        // Add flags to the intent:
        // FLAG_ACTIVITY_CLEAR_TOP: If HomeActivity is already in the stack, clear all activities above it.
        // FLAG_ACTIVITY_SINGLE_TOP: Don't create a new instance of HomeActivity if it's already on top.
        // Together, these flags ensure we return to the existing Home instance without messing up the navigation stack.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        startActivity(intent);

        // Finish (destroy) the current MainActivity. This is essential to ensure that when the user presses "back" from the new
        // game, they don't return to an old, finished game screen.
        finish();
    }

    /**
     * Initializes the Sudoku grid overlay by creating 81 TextViews.
     * 
     * @param cellSize The calculated size for each cell.
     */
    private void initializeSudokuGridOverlay(int cellSize) {
        binding.sudokuGridOverlay.removeAllViews(); // Clear if already initialized (e.g., rotation)
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                // To use variables in a lambda, they must be 'effectively final'.
                // So we create final copies for use in the onClickListener.
                final int finalRow = row;
                final int finalCol = col;

                TextView cellView = new TextView(this);
                cellView.setWidth(cellSize);
                cellView.setHeight(cellSize);
                cellView.setGravity(Gravity.CENTER);
                cellView.setIncludeFontPadding(false);
                cellView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (cellSize / 4.0));
                cellView.setTextColor(ContextCompat.getColor(this, R.color.text_brown));

                cellView.setOnClickListener(v -> viewModel.selectCell(finalRow, finalCol));

                binding.sudokuGridOverlay.addView(cellView);
                cellTextViews[row][col] = cellView;
            }
        }
        // After initialization, populate with the current data from the ViewModel.
        if (viewModel.getSudokuBoard().getValue() != null) {
            updateGridUI(viewModel.getSudokuBoard().getValue());
        }
        if (viewModel.getSelectedCell().getValue() != null) {
            Pair<Integer, Integer> selection = viewModel.getSelectedCell().getValue();
            // updateSelectedCellUI(selection.first, selection.second);
        }
    }

    /**
     * Sets up observers for the LiveData from the SudokuViewModel. Updates the UI whenever the data changes.
     */
    private void observeViewModel() {
        // Observe board changes
        viewModel.getSudokuBoard().observe(this, board -> {
            if (board != null) {
                binding.difficultyText
                        .setText(getDifficultyStringRes(board.getCurrentDifficulty()));
                updateGridUI(board);
            }
        });

        // Observe cell selection changes
        viewModel.getSelectedCell().observe(this, selection -> {
            float cellSize = (binding.sudokuContainer.getWidth() > 0)
                    ? (float) binding.sudokuContainer.getWidth() / 9.0f
                    : 0f;
            if (selection != null) {
                highlightOverlayView.highlightCell(selection.first, selection.second, cellSize);
            } else {
                highlightOverlayView.highlightCell(null, null, cellSize);
            }

            // We can remove the old highlight logic from TextView
            // updateSelectedCellUI(null, null);
        });

        // Observe timer
        viewModel.getElapsedTimeInMillis().observe(this, timeInMillis -> {
            int minutes = (int) (timeInMillis / 60000);
            int seconds = (int) ((timeInMillis % 60000) / 1000);
            binding.timerText
                    .setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        });

        // Observe error count
        viewModel.getErrorCount().observe(this,
                count -> binding.errorText.setText(getString(R.string.errors_format, count)));

        // Observe score
        viewModel.getScore().observe(this,
                score -> binding.scoreText.setText(getString(R.string.score_format, score)));

        // Observe game won state
        viewModel.isGameWon().observe(this, isWon -> {
            if (isWon != null && isWon) {
                showGameOverDialog(getString(R.string.game_over_congratulations_title),
                        getString(R.string.game_over_success_message));
            }
        });

        // Observe game over state (incorrect board)
        viewModel.isGameOverWithIncorrectBoard().observe(this, isIncorrect -> {
            if (isIncorrect != null && isIncorrect) {
                showGameOverDialog(getString(R.string.game_over_oops_title),
                        getString(R.string.game_over_fail_message));
            }
        });

        // Observe puzzle generation state
        viewModel.isGenerating().observe(this, isGenerating -> {
            if (isGenerating != null && isGenerating) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.sudokuContainer.setVisibility(View.INVISIBLE);
                binding.numberPad.setVisibility(View.INVISIBLE);
                binding.undoButton.setEnabled(false);
                binding.newGameButton.setEnabled(false);
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.sudokuContainer.setVisibility(View.VISIBLE);
                binding.numberPad.setVisibility(View.VISIBLE);
                binding.undoButton.setEnabled(true);
                binding.newGameButton.setEnabled(true);
            }
        });
    }

    /**
     * Updates the entire TextView grid with values from the SudokuBoard.
     * 
     * @param board The SudokuBoard with the current data.
     */
    @SuppressLint("SetTextI18n")
    private void updateGridUI(SudokuBoard board) {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                SudokuCell cell = board.getCell(row, col);
                TextView cellView = cellTextViews[row][col];
                if (cell != null && cellView != null) {
                    if (cell.getValue() == 0) {
                        cellView.setText("");
                    } else {
                        cellView.setText(Integer.toString(cell.getValue()));
                    }

                    // Set text color based on cell state
                    int textColorRes;
                    if (cell.isFixed()) {
                        textColorRes = R.color.text_brown;
                    } else if (cell.getValue() == 0) {
                        textColorRes = R.color.text_brown;
                    } else if (cell.isCorrect()) {
                        textColorRes = R.color.accent_teal;
                    } else {
                        textColorRes = R.color.error_red;
                    }
                    cellView.setTextColor(ContextCompat.getColor(this, textColorRes));
                }
            }
        }
        // Re-apply highlight on the selected cell, if any
        Pair<Integer, Integer> selection = viewModel.getSelectedCell().getValue();
        // updateSelectedCellUI(selection != null ? selection.first : null, selection != null ? selection.second : null);
    }

    /**
     * Shows a dialog to choose the difficulty and start a new game.
     */
    private void showNewGameDialog() {
        new AlertDialog.Builder(this).setTitle(getString(R.string.new_game_dialog_title))
                .setMessage(getString(R.string.new_game_dialog_message))
                // The three buttons now use lambdas for their onClick logic.
                .setPositiveButton(getString(R.string.difficulty_easy),
                        (dialog, which) -> viewModel.startNewGame(SudokuBoard.Difficulty.EASY))
                .setNeutralButton(getString(R.string.difficulty_medium),
                        (dialog, which) -> viewModel.startNewGame(SudokuBoard.Difficulty.MEDIUM))
                .setNegativeButton(getString(R.string.difficulty_hard),
                        (dialog, which) -> viewModel.startNewGame(SudokuBoard.Difficulty.HARD))
                .setCancelable(true).show();
    }

    /**
     * Shows a game over dialog.
     * 
     * @param title The title of the dialog.
     * @param message The main message of the dialog.
     */
    private void showGameOverDialog(String title, String message) {
        int finalScore = Objects.requireNonNullElse(viewModel.getScore().getValue(), 0);
        String fullMessage =
                message + " " + getString(R.string.game_over_final_score_format, finalScore);

        new AlertDialog.Builder(this).setTitle(title).setMessage(fullMessage)
                .setPositiveButton(getString(R.string.new_game_button),
                        (dialog, which) -> showNewGameDialog())
                .setNegativeButton(getString(R.string.close_button),
                        (dialog, which) -> dialog.dismiss())
                .setCancelable(false) // Prevents closing with the back button until a choice is
                                      // made
                .show();
    }
}
