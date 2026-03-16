package com.example.sudoku;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;

import com.example.sudoku.databinding.ActivityMainBinding;
import com.example.sudoku.viewmodel.SudokuViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;
import java.util.Objects;

/**
 * The main activity for the Sudoku game. This class is responsible for setting up the UI, observing the game state from
 * the SudokuViewModel, and handling user interactions like button presses and cell selections.
 */
public class MainActivity extends AppCompatActivity {

    // Intent keys
    public static final String EXTRA_DIFFICULTY = "com.example.sudoku.DIFFICULTY";
    public static final String EXTRA_RESUME_SAVED_GAME = "com.example.sudoku.RESUME_SAVED_GAME";
    // Bundle keys
    private static final String KEY_SUDOKU_BOARD_STATE = "sudokuBoardState";
    private static final String KEY_VIEW_MODEL_BUNDLE_STATE = "viewModelBundleState";

    // ViewBinding and ViewModel declaration.
    private ActivityMainBinding binding;
    private SudokuViewModel viewModel;
    private HighlightOverlayView highlightOverlayView;

    // Array to hold the grid TextViews for quick access.
    private final TextView[][] cellTextViews = new TextView[9][9];
    private final Button[] numberPadButtons = new Button[9];
    private boolean shouldPersistOnStop = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout using ViewBinding.
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get the ViewModel instance using ViewModelProvider.
        viewModel = new ViewModelProvider(this).get(SudokuViewModel.class);

        // Initialize the HighlightOverlayView.
        highlightOverlayView = binding.highlightOverlayView;

        // State restoration logic.
        if (savedInstanceState == null) {
            if (!restoreSavedGameIfRequested()) {
                // We explicitly tell the ViewModel to start a new game.
                viewModel.startNewGame(resolveLaunchDifficulty());
            }

        } else {
            // Restore state after process death.
            SudokuBoard boardState = savedInstanceState.getParcelable(KEY_SUDOKU_BOARD_STATE, SudokuBoard.class);
            Bundle viewModelBundle = savedInstanceState.getBundle(KEY_VIEW_MODEL_BUNDLE_STATE);

            if (boardState != null && viewModelBundle != null) {
                viewModel.restoreState(boardState, viewModelBundle);
            } else {
                // Fallback: if state is missing, start a new game.
                viewModel.startNewGame(resolveLaunchDifficulty());
            }
        }

        setupNumberPad();
        setupActionButtons();
        observeViewModel();

        // Use a ViewTreeObserver to create the grid TextViews once the container's size is known.
        binding.sudokuContainer.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        binding.sudokuContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        initializeSudokuGridOverlay(binding.sudokuContainer.getWidth() / 9);
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Pair<SudokuBoard, Bundle> state = viewModel.saveState();
        if (state != null && state.first != null) {
            outState.putParcelable(KEY_SUDOKU_BOARD_STATE, state.first);
            outState.putBundle(KEY_VIEW_MODEL_BUNDLE_STATE, state.second);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        persistCurrentGameIfNeeded();
    }

    private int getDifficultyStringRes(SudokuBoard.Difficulty difficulty) {
        if (difficulty == null) {
            return R.string.difficulty_medium;
        }
        return switch (difficulty) {
            case EASY -> R.string.difficulty_easy;
            case HARD -> R.string.difficulty_hard;
            default -> R.string.difficulty_medium;
        };
    }

    private SudokuBoard.Difficulty resolveLaunchDifficulty() {
        SudokuBoard.Difficulty difficulty = getIntent().getSerializableExtra(EXTRA_DIFFICULTY, SudokuBoard.Difficulty.class);
        return difficulty != null ? difficulty : SudokuBoard.Difficulty.MEDIUM;
    }

    private boolean restoreSavedGameIfRequested() {
        if (!getIntent().getBooleanExtra(EXTRA_RESUME_SAVED_GAME, false)) {
            return false;
        }

        SavedGameStore.SavedGame savedGame = SavedGameStore.load(this);
        if (savedGame == null) {
            return false;
        }

        viewModel.restoreState(savedGame.getBoard(), savedGame.getViewModelState());
        return true;
    }

    /**
     * Initializes the number pad with refined styling and margins.
     */
    private void setupNumberPad() {
        int margin = dpToPx(4);
        for (int i = 1; i <= 9; i++) {
            final int numberToInput = i;

            Button button = new Button(this);
            button.setText(String.valueOf(i));
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
            button.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
            button.setTransformationMethod(null);
            button.setPadding(0, dpToPx(8), 0, dpToPx(8));
            button.setOnClickListener(v -> viewModel.inputNumber(numberToInput));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(margin, margin, margin, margin);
            button.setLayoutParams(params);

            button.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_number_pad_button));
            button.setTextColor(ContextCompat.getColor(this, R.color.onSurfaceVariant));
            button.setElevation(dpToPx(2));

            binding.numberPad.addView(button);
            numberPadButtons[i - 1] = button;
        }
    }

    private void setupActionButtons() {
        binding.clearButton.setOnClickListener(v -> {
            if (!viewModel.clearSelectedCell()) {
                Toast.makeText(this, getString(R.string.nothing_to_clear), Toast.LENGTH_SHORT).show();
            }
        });
        binding.undoButton.setOnClickListener(v -> {
            if (!viewModel.undoLastMove()) {
                Toast.makeText(this, getString(R.string.no_moves_to_undo), Toast.LENGTH_SHORT).show();
            }
        });
        binding.newGameButton.setOnClickListener(v -> showNewGameOptionsDialog());
    }

    private void returnToHome() {
        shouldPersistOnStop = false;
        SavedGameStore.clear(this);
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void initializeSudokuGridOverlay(int cellSize) {
        binding.sudokuGridOverlay.removeAllViews();
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                final int finalRow = row;
                final int finalCol = col;

                TextView cellView = new TextView(this);
                cellView.setWidth(cellSize);
                cellView.setHeight(cellSize);
                cellView.setGravity(Gravity.CENTER);
                cellView.setIncludeFontPadding(false);
                cellView.setTextSize(TypedValue.COMPLEX_UNIT_SP, pixelsToScaledSp(cellSize * 0.65f));
                cellView.setTextColor(ContextCompat.getColor(this, R.color.onBackground));
                cellView.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));

                cellView.setOnClickListener(v -> viewModel.selectCell(finalRow, finalCol));

                binding.sudokuGridOverlay.addView(cellView);
                cellTextViews[row][col] = cellView;
            }
        }
        if (viewModel.getSudokuBoard().getValue() != null) {
            updateGridUI(viewModel.getSudokuBoard().getValue());
        }
        if (viewModel.getSelectedCell().getValue() != null) {
            updateHighlightOverlay(viewModel.getSelectedCell().getValue());
        }
        refreshInteractiveControls();
    }

    private void observeViewModel() {
        viewModel.getSudokuBoard().observe(this, board -> {
            if (board != null) {
                binding.difficultyText.setText(getDifficultyStringRes(board.getCurrentDifficulty()));
                updateGridUI(board);
            }
            refreshInteractiveControls();
        });

        viewModel.getSelectedCell().observe(this, selection -> {
            updateHighlightOverlay(selection);
            refreshInteractiveControls();
        });

        viewModel.getElapsedTimeInMillis().observe(this, timeInMillis -> {
            int minutes = (int) (timeInMillis / 60000);
            int seconds = (int) ((timeInMillis % 60000) / 1000);
            binding.timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        });

        viewModel.getErrorCount().observe(this,
                count -> binding.errorText.setText(getString(R.string.errors_format, count)));

        viewModel.getScore().observe(this, score -> binding.scoreText.setText(String.valueOf(score)));

        viewModel.isGameWon().observe(this, isWon -> {
            if (isWon != null && isWon) {
                recordWinStatsIfNeeded();
                SavedGameStore.clear(this);
                showGameOverDialog(getString(R.string.game_over_congratulations_title),
                        getString(R.string.game_over_success_message));
            }
        });

        viewModel.isGameOverWithIncorrectBoard().observe(this, isIncorrect -> {
            if (isIncorrect != null && isIncorrect) {
                SavedGameStore.clear(this);
                showGameOverDialog(getString(R.string.game_over_oops_title),
                        getString(R.string.game_over_fail_message));
            }
        });

        viewModel.isGenerating().observe(this, isGenerating -> {
            int visibility = (isGenerating != null && isGenerating) ? View.INVISIBLE : View.VISIBLE;
            int loadingVisibility = (isGenerating != null && isGenerating) ? View.VISIBLE : View.GONE;

            binding.loadingStateContainer.setVisibility(loadingVisibility);
            binding.progressBar.setVisibility(loadingVisibility);

            binding.sudokuGridView.setVisibility(visibility);
            binding.highlightOverlayView.setVisibility(visibility);
            binding.sudokuGridOverlay.setVisibility(visibility);
            binding.numberPad.setVisibility(visibility);

            refreshInteractiveControls();
        });

        viewModel.getGenerationErrorMessage().observe(this, messageResId -> {
            if (messageResId == null) {
                return;
            }
            Toast.makeText(this, getString(messageResId), Toast.LENGTH_LONG).show();
            viewModel.clearGenerationErrorMessage();
        });
    }

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

                    int textColorRes;
                    if (cell.isFixed()) {
                        textColorRes = R.color.onBackground;
                    } else if (cell.getValue() == 0) {
                        textColorRes = R.color.onBackground;
                    } else if (cell.isCorrect()) {
                        textColorRes = R.color.primary;
                    } else {
                        textColorRes = R.color.error;
                    }

                    int newColor = ContextCompat.getColor(this, textColorRes);
                    int oldColor = cellView.getCurrentTextColor();

                    if (oldColor != newColor) {
                        android.animation.ObjectAnimator.ofArgb(cellView, "textColor", oldColor, newColor)
                                .setDuration(200)
                                .start();
                    } else {
                        cellView.setTextColor(newColor);
                    }

                    cellView.setTypeface(Typeface.create(Typeface.SERIF, cell.isFixed() ? Typeface.BOLD : Typeface.NORMAL));
                }
            }
        }
    }

    private void updateHighlightOverlay(Pair<Integer, Integer> selection) {
        float cellSize = (binding.sudokuContainer.getWidth() > 0) ? (float) binding.sudokuContainer.getWidth() / 9.0f : 0f;
        if (selection != null) {
            highlightOverlayView.highlightCell(selection.first, selection.second, cellSize);
        } else {
            highlightOverlayView.highlightCell(null, null, cellSize);
        }
    }

    private void refreshInteractiveControls() {
        boolean isGenerating = Boolean.TRUE.equals(viewModel.isGenerating().getValue());
        SudokuCell selectedEditableCell = getSelectedEditableCell();
        boolean hasEditableSelection = selectedEditableCell != null;
        boolean canClear = hasEditableSelection
                && (selectedEditableCell.getValue() != 0 || !selectedEditableCell.getNotes().isEmpty());

        for (Button button : numberPadButtons) {
            if (button != null) {
                updateControlState(button, !isGenerating && hasEditableSelection);
            }
        }

        updateControlState(binding.clearButton, !isGenerating && canClear);
        updateControlState(binding.undoButton, !isGenerating);
        updateControlState(binding.newGameButton, !isGenerating);
    }

    private SudokuCell getSelectedEditableCell() {
        SudokuBoard board = viewModel.getSudokuBoard().getValue();
        Pair<Integer, Integer> selection = viewModel.getSelectedCell().getValue();
        if (board == null || selection == null) {
            return null;
        }
        SudokuCell cell = board.getCell(selection.first, selection.second);
        if (cell == null || cell.isFixed()) {
            return null;
        }
        return cell;
    }

    private SudokuBoard.Difficulty getCurrentDifficulty() {
        SudokuBoard board = viewModel.getSudokuBoard().getValue();
        return board != null ? board.getCurrentDifficulty() : resolveLaunchDifficulty();
    }

    private void recordWinStatsIfNeeded() {
        SudokuBoard board = viewModel.getSudokuBoard().getValue();
        if (board == null || !viewModel.markWinStatsRecordedIfNeeded()) {
            return;
        }
        long elapsedTimeInMillis = Objects.requireNonNullElse(viewModel.getElapsedTimeInMillis().getValue(), 0L);
        int finalScore = Objects.requireNonNullElse(viewModel.getScore().getValue(), 0);
        GameStatsStore.recordWin(this, board.getCurrentDifficulty(), elapsedTimeInMillis, finalScore);
    }

    private void persistCurrentGameIfNeeded() {
        if (!shouldPersistOnStop) {
            SavedGameStore.clear(this);
            return;
        }
        if (Boolean.TRUE.equals(viewModel.isGenerating().getValue())
                || Boolean.TRUE.equals(viewModel.isGameWon().getValue())
                || Boolean.TRUE.equals(viewModel.isGameOverWithIncorrectBoard().getValue())) {
            SavedGameStore.clear(this);
            return;
        }
        Pair<SudokuBoard, Bundle> state = viewModel.saveState();
        if (state == null || state.first == null || state.second == null) {
            SavedGameStore.clear(this);
            return;
        }
        SavedGameStore.save(this, state.first, state.second);
    }

    private void showNewGameOptionsDialog() {
        String difficultyLabel = getString(getDifficultyStringRes(getCurrentDifficulty()));
        new MaterialAlertDialogBuilder(this).setTitle(getString(R.string.new_game_dialog_title))
                .setMessage(getString(R.string.new_game_same_difficulty_message, difficultyLabel))
                .setPositiveButton(getString(R.string.restart_button),
                        (dialog, which) -> {
                            SavedGameStore.clear(this);
                            viewModel.startNewGame(getCurrentDifficulty());
                        })
                .setNeutralButton(getString(R.string.back_to_home_button), (dialog, which) -> returnToHome())
                .setNegativeButton(getString(R.string.cancel_button), null)
                .show();
    }

    private void showGameOverDialog(String title, String message) {
        int finalScore = Objects.requireNonNullElse(viewModel.getScore().getValue(), 0);
        String fullMessage = getString(R.string.game_over_message_with_score, message,
                getString(R.string.game_over_final_score_format, finalScore));

        new MaterialAlertDialogBuilder(this).setTitle(title).setMessage(fullMessage)
                .setPositiveButton(getString(R.string.play_again_button),
                        (dialog, which) -> {
                            SavedGameStore.clear(this);
                            viewModel.startNewGame(getCurrentDifficulty());
                        })
                .setNeutralButton(getString(R.string.back_to_home_button), (dialog, which) -> returnToHome())
                .setNegativeButton(getString(R.string.close_button), (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void updateControlState(@NonNull View view, boolean enabled) {
        view.setEnabled(enabled);
        view.animate().alpha(enabled ? 1f : 0.35f).setDuration(200).start();
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    private float pixelsToScaledSp(float pixels) {
        return pixels / getResources().getDisplayMetrics().density;
    }
}
