package com.example.sudoku.viewmodel;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sudoku.SudokuBoard;
import com.example.sudoku.SudokuCell;

import java.util.Objects;

/**
 * ViewModel for the Sudoku game.
 * It holds the game state, handles user interactions, and communicates with the UI (Activity/Fragment)
 * via LiveData, ensuring that the state survives configuration changes.
 * This is the Java translation of the original Kotlin ViewModel.
 */
public class SudokuViewModel extends ViewModel {

    // --- LiveData Fields ---
    // The private MutableLiveData can be changed only within this ViewModel.
    private final MutableLiveData<SudokuBoard> _sudokuBoard = new MutableLiveData<>();
    private final MutableLiveData<Pair<Integer, Integer>> _selectedCell = new MutableLiveData<>();
    private final MutableLiveData<Long> _elapsedTimeInMillis = new MutableLiveData<>(0L);
    private final MutableLiveData<Integer> _errorCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> _score = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> _isGameWon = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isGameOverWithIncorrectBoard = new MutableLiveData<>(false);

    // --- Timer related fields ---
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private long chronometerBase = 0L;
    private boolean isTimerRunning = false;

    /**
     * The public, immutable LiveData that the UI can observe.
     * This follows the recommended pattern of exposing only read-only LiveData to observers.
     */
    public LiveData<SudokuBoard> getSudokuBoard() { return _sudokuBoard; }
    public LiveData<Pair<Integer, Integer>> getSelectedCell() { return _selectedCell; }
    public LiveData<Long> getElapsedTimeInMillis() { return _elapsedTimeInMillis; }
    public LiveData<Integer> getErrorCount() { return _errorCount; }
    public LiveData<Integer> getScore() { return _score; }
    public LiveData<Boolean> isGameWon() { return _isGameWon; }
    public LiveData<Boolean> isGameOverWithIncorrectBoard() { return _isGameOverWithIncorrectBoard; }

    /**
     * Constructor for the ViewModel.
     * Corresponds to the `init` block in Kotlin. It starts a new game upon creation.
     */
    public SudokuViewModel() {
        // startNewGame(SudokuBoard.Difficulty.MEDIUM);
    }

    /**
     * Starts a new Sudoku game with the specified difficulty.
     * @param difficulty The desired difficulty level.
     */
    public void startNewGame(SudokuBoard.Difficulty difficulty) {
        SudokuBoard newBoard = new SudokuBoard();
        newBoard.generateNewPuzzle(difficulty);
        _sudokuBoard.setValue(newBoard);
        _selectedCell.setValue(null);
        _errorCount.setValue(0);
        _score.setValue(0);
        _isGameWon.setValue(false);
        _isGameOverWithIncorrectBoard.setValue(false);
        resetAndStartTimer();
        updateScore();
    }

    /**
     * Selects a cell on the grid.
     * @param row The row of the selected cell (0-8).
     * @param col The column of the selected cell (0-8).
     */
    public void selectCell(int row, int col) {
        _selectedCell.setValue(new Pair<>(row, col));
    }

    /**
     * Inputs a number into the currently selected cell.
     * If the move is invalid, it increments the error count.
     * Updates the UI and checks the game status.
     * @param number The number to input (1-9). If 0, the cell is cleared.
     */
    public void inputNumber(int number) {
        SudokuBoard board = _sudokuBoard.getValue();
        Pair<Integer, Integer> selection = _selectedCell.getValue();

        if (board == null || selection == null) return;

        int row = selection.first;
        int col = selection.second;
        SudokuCell cell = board.getCell(row, col);

        if (cell != null && !cell.isFixed()) {
            board.setCellValue(row, col, number); // This method updates the cell's isCorrect flag.

            _errorCount.setValue(board.countUserErrors());
            _sudokuBoard.setValue(board); // Notify observers that the board has changed.
            updateScore();
            checkGameStatus();
        }
    }

    /**
     * Undoes the last move.
     * @return True if a move was undone, false otherwise.
     */
    public boolean undoLastMove() {
        SudokuBoard board = _sudokuBoard.getValue();
        if (board == null) return false;

        boolean success = board.undoMove();
        if (success) {
            _sudokuBoard.setValue(board); // Notify observers
            _errorCount.setValue(board.countUserErrors());
            updateScore();

            // The game might no longer be in a won/lost state after an undo.
            _isGameWon.setValue(false);
            _isGameOverWithIncorrectBoard.setValue(false);

            // If the timer was stopped because the game was over, restart it.
            if (!isTimerRunning && !board.isBoardFull()) {
                startTimerIfNotRunning();
            }
        }
        return success;
    }

    /**
     * Saves the current state of the ViewModel.
     * Used for onSaveInstanceState in the Activity.
     * @return A Pair containing the SudokuBoard state and a Bundle with other state information.
     */
    public Pair<SudokuBoard, Bundle> saveState() {
        Bundle bundle = new Bundle();
        Pair<Integer, Integer> selection = _selectedCell.getValue();
        if (selection != null) {
            bundle.putInt("selectedRow", selection.first);
            bundle.putInt("selectedCol", selection.second);
        }
        bundle.putLong("chronometerBase", chronometerBase);
        bundle.putBoolean("isTimerRunning", isTimerRunning);
        bundle.putLong("elapsedTimeInMillis", Objects.requireNonNullElse(_elapsedTimeInMillis.getValue(), 0L));
        bundle.putInt("errorCount", Objects.requireNonNullElse(_errorCount.getValue(), 0));

        return new Pair<>(_sudokuBoard.getValue(), bundle);
    }

    /**
     * Restores the state of the ViewModel.
     * Used from onCreate or onRestoreInstanceState in the Activity.
     * @param boardState The SudokuBoard state to restore.
     * @param bundleState Bundle containing other state information.
     */
    public void restoreState(SudokuBoard boardState, Bundle bundleState) {
        if (boardState != null) {
            _sudokuBoard.setValue(boardState);
        }

        int savedSelectedRow = bundleState.getInt("selectedRow", -1);
        int savedSelectedCol = bundleState.getInt("selectedCol", -1);
        if (savedSelectedRow != -1 && savedSelectedCol != -1) {
            _selectedCell.setValue(new Pair<>(savedSelectedRow, savedSelectedCol));
        } else {
            _selectedCell.setValue(null);
        }

        chronometerBase = bundleState.getLong("chronometerBase", SystemClock.elapsedRealtime());
        isTimerRunning = bundleState.getBoolean("isTimerRunning", false);
        _elapsedTimeInMillis.setValue(bundleState.getLong("elapsedTimeInMillis", 0L));
        _errorCount.setValue(bundleState.getInt("errorCount",
                _sudokuBoard.getValue() != null ? _sudokuBoard.getValue().countUserErrors() : 0));

        updateScore();

        // If the game was running and not finished, restart the timer.
        if (isTimerRunning) {
            startTimerIfNotRunning();
        } else {
            stopTimer();
            // If the board is full, re-check the game status (e.g., after rotation).
            if (_sudokuBoard.getValue() != null && _sudokuBoard.getValue().isBoardFull()) {
                checkGameStatus();
            }
        }
    }


    /* --- Private Helper Methods --- */

    private void startTimerIfNotRunning() {
        if (!isTimerRunning) {
            if (chronometerBase == 0L) {
                // On first start or after a reset
                chronometerBase = SystemClock.elapsedRealtime() - Objects.requireNonNullElse(_elapsedTimeInMillis.getValue(), 0L);
            }
            // otherwise, chronometerBase is already correctly set from a restored state.

            isTimerRunning = true;
            // Define the runnable that updates the timer
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isTimerRunning) {
                        long now = SystemClock.elapsedRealtime();
                        _elapsedTimeInMillis.postValue(now - chronometerBase);
                        // Schedule the next run
                        timerHandler.postDelayed(this, 1000);
                    }
                }
            };
            // Start the timer immediately
            timerHandler.post(timerRunnable);
        }
    }

    private void stopTimer() {
        isTimerRunning = false;
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void resetAndStartTimer() {
        stopTimer();
        _elapsedTimeInMillis.setValue(0L);
        chronometerBase = 0L; // Reset base to ensure it's recalculated on start
        startTimerIfNotRunning();
    }

    /**
     * Calculates the score based on difficulty, time, and errors.
     */
    private void updateScore() {
        SudokuBoard board = _sudokuBoard.getValue();
        if (board == null) return;

        long timeInMillis = Objects.requireNonNullElse(_elapsedTimeInMillis.getValue(), 0L);
        int errors = Objects.requireNonNullElse(_errorCount.getValue(), 0);

        long timePenalty = timeInMillis / 1000 / 2; // 1 point every 2 seconds
        int errorPenalty = errors * 50;

        int difficultyMultiplier = switch (board.getCurrentDifficulty()) {
            case EASY -> 1;
            case HARD -> 3;
            default -> // MEDIUM
                    2;
        };
        int baseScore = 10000;

        int finalScore = (int) ((baseScore * difficultyMultiplier) - timePenalty - errorPenalty);
        _score.setValue(Math.max(0, finalScore));
    }

    /**
     * Checks if the game is over (either won or the board is full but incorrect).
     * Updates the relevant LiveData for the UI to observe.
     */
    private void checkGameStatus() {
        SudokuBoard board = _sudokuBoard.getValue();
        if (board == null) return;

        if (board.isBoardFull()) {
            stopTimer();
            // Full check: board is valid by rules AND all user numbers are correct against the solution
            if (board.isCurrentBoardStateValidAccordingToRules() && board.areAllUserCellsCorrect()) {
                _isGameWon.setValue(true);
            } else {
                _isGameOverWithIncorrectBoard.setValue(true);
            }
        } else {
            startTimerIfNotRunning();
        }
    }

    /**
     * This method is called when the ViewModel is about to be destroyed.
     * It's the perfect place to clean up resources, like stopping the timer handler.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        stopTimer(); // Stop the handler callbacks to prevent memory leaks.
    }
}