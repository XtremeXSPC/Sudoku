package com.example.sudoku;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
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
 * The main activity for the Sudoku game.
 * This class is responsible for setting up the UI, observing the game state from the
 * SudokuViewModel, and handling user interactions like button presses and cell selections.
 */
public class MainActivity extends AppCompatActivity {

    // ViewBinding and ViewModel declaration.
    private ActivityMainBinding binding;
    private SudokuViewModel viewModel;

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

        // State restoration logic.
        if (savedInstanceState != null) {
            SudokuBoard boardState;
            // Handle the modern and deprecated getParcelable methods based on API level.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                boardState = savedInstanceState.getParcelable("sudokuBoardState", SudokuBoard.class);
            } else {
                // Suppress the deprecation warning for older APIs.
                boardState = savedInstanceState.getParcelable("sudokuBoardState");
            }

            Bundle bundleState = savedInstanceState.getBundle("viewModelBundleState");
            if (bundleState != null) {
                viewModel.restoreState(boardState, bundleState);
            }
        }

        setupNumberPad();
        setupActionButtons();
        observeViewModel();

        // Use a ViewTreeObserver to create the grid TextViews once the container's size is known.
        // In Java, we use an anonymous inner class for the listener.
        binding.sudokuContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // It's crucial to remove the listener to prevent it from being called multiple times.
                binding.sudokuContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                initializeSudokuGridOverlay(binding.sudokuContainer.getWidth() / 9);

                // Force an initial UI update based on ViewModel data, especially useful after a rotation.
                if (viewModel.getSudokuBoard().getValue() != null) {
                    updateGridUI(viewModel.getSudokuBoard().getValue());
                }
                if (viewModel.getSelectedCell().getValue() != null) {
                    Pair<Integer, Integer> selection = viewModel.getSelectedCell().getValue();
                    updateSelectedCellUI(selection.first, selection.second);
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Get the state from the ViewModel. Java doesn't have destructuring declarations.
        Pair<SudokuBoard, Bundle> state = viewModel.saveState();
        if (state.first != null) {
            outState.putParcelable("sudokuBoardState", state.first);
        }
        outState.putBundle("viewModelBundleState", state.second);
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

            button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.button_beige));
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
                Toast.makeText(this, getString(R.string.no_moves_to_undo), Toast.LENGTH_SHORT).show();
            }
        });
        binding.newGameButton.setOnClickListener(v -> showNewGameDialog());
    }

    /**
     * Initializes the Sudoku grid overlay by creating 81 TextViews.
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
                cellView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (cellSize / 3.5));
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
            updateSelectedCellUI(selection.first, selection.second);
        }
    }

    /**
     * Sets up observers for the LiveData from the SudokuViewModel.
     * Updates the UI whenever the data changes.
     */
    private void observeViewModel() {
        // Observe board changes
        viewModel.getSudokuBoard().observe(this, this::updateGridUI);

        // Observe cell selection changes
        viewModel.getSelectedCell().observe(this, selection -> {
            if (selection != null) {
                updateSelectedCellUI(selection.first, selection.second);
            } else {
                updateSelectedCellUI(null, null);
            }
        });

        // Observe timer
        viewModel.getElapsedTimeInMillis().observe(this, timeInMillis -> {
            int minutes = (int) (timeInMillis / 60000);
            int seconds = (int) ((timeInMillis % 60000) / 1000);
            binding.timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        });

        // Observe error count
        viewModel.getErrorCount().observe(this, count ->
                binding.errorText.setText(getString(R.string.errors_format, count)));

        // Observe score
        viewModel.getScore().observe(this, score ->
                binding.scoreText.setText(getString(R.string.score_format, score)));

        // Observe game won state
        viewModel.isGameWon().observe(this, isWon -> {
            if (isWon != null && isWon) {
                showGameOverDialog(
                        getString(R.string.game_over_congratulations_title),
                        getString(R.string.game_over_success_message)
                );
            }
        });

        // Observe game over state (incorrect board)
        viewModel.isGameOverWithIncorrectBoard().observe(this, isIncorrect -> {
            if (isIncorrect != null && isIncorrect) {
                showGameOverDialog(
                        getString(R.string.game_over_oops_title),
                        getString(R.string.game_over_fail_message)
                );
            }
        });
    }

    /**
     * Updates the entire TextView grid with values from the SudokuBoard.
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
        updateSelectedCellUI(selection != null ? selection.first : null, selection != null ? selection.second : null);
    }

    /**
     * Updates the UI to highlight the selected cell and deselect others.
     * @param selectedRow Row of the selected cell, or null if none.
     * @param selectedCol Column of the selected cell, or null if none.
     */
    private void updateSelectedCellUI(Integer selectedRow, Integer selectedCol) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                TextView cellView = cellTextViews[r][c];
                if (cellView != null) {
                    boolean isSelected = (selectedRow != null && selectedCol != null && r == selectedRow && c == selectedCol);
                    if (isSelected) {
                        cellView.setBackgroundResource(R.drawable.cell_background_selected);
                    } else {
                        // The SudokuGridView handles the light/dark block backgrounds.
                        // The TextViews here must be transparent so as not to cover them.
                        cellView.setBackgroundResource(android.R.color.transparent);
                    }
                }
            }
        }
    }


    /**
     * Shows a dialog to choose the difficulty and start a new game.
     */
    private void showNewGameDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.new_game_dialog_title))
                .setMessage(getString(R.string.new_game_dialog_message))
                // The three buttons now use lambdas for their onClick logic.
                .setPositiveButton(getString(R.string.difficulty_easy), (dialog, which) ->
                        viewModel.startNewGame(SudokuBoard.Difficulty.EASY))
                .setNeutralButton(getString(R.string.difficulty_medium), (dialog, which) ->
                        viewModel.startNewGame(SudokuBoard.Difficulty.MEDIUM))
                .setNegativeButton(getString(R.string.difficulty_hard), (dialog, which) ->
                        viewModel.startNewGame(SudokuBoard.Difficulty.HARD))
                .setCancelable(true)
                .show();
    }

    /**
     * Shows a game over dialog.
     * @param title The title of the dialog.
     * @param message The main message of the dialog.
     */
    private void showGameOverDialog(String title, String message) {
        int finalScore = Objects.requireNonNullElse(viewModel.getScore().getValue(), 0);
        String fullMessage = message + " " + getString(R.string.game_over_final_score_format, finalScore);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(fullMessage)
                .setPositiveButton(getString(R.string.new_game_button), (dialog, which) -> showNewGameDialog())
                .setNegativeButton(getString(R.string.close_button), (dialog, which) -> dialog.dismiss())
                .setCancelable(false) // Prevents closing with the back button until a choice is made
                .show();
    }
}