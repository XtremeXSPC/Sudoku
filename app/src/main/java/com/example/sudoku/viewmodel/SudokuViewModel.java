package com.example.sudoku.viewmodel;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sudoku.R;
import com.example.sudoku.SudokuBoard;
import com.example.sudoku.SudokuCell;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ViewModel for the Sudoku game. It holds the game state, handles user interactions, and communicates with
 * the UI (Activity/Fragment) via LiveData, ensuring that the state survives configuration changes. This is
 * the Java translation of the original Kotlin ViewModel.
 */
public class SudokuViewModel extends ViewModel {

    private static final String STATE_SELECTED_ROW = "selectedRow";
    private static final String STATE_SELECTED_COL = "selectedCol";
    private static final String STATE_CHRONOMETER_BASE = "chronometerBase";
    private static final String STATE_IS_TIMER_RUNNING = "isTimerRunning";
    private static final String STATE_ELAPSED_TIME_IN_MILLIS = "elapsedTimeInMillis";
    private static final String STATE_ERROR_COUNT = "errorCount";
    private static final String STATE_TOTAL_ERRORS = "totalErrorsThisGame";
    private static final String STATE_SCORE = "score";
    private static final String STATE_IS_GAME_WON = "isGameWon";
    private static final String STATE_IS_GAME_OVER_WITH_INCORRECT_BOARD = "isGameOverWithIncorrectBoard";
    private static final String STATE_COMPLETION_BONUS_APPLIED = "completionBonusApplied";
    private static final String STATE_AWARDED_COMPLETION_BONUS = "awardedCompletionBonus";
    private static final String STATE_WIN_STATS_RECORDED = "winStatsRecorded";

    /* ----- LiveData Fields ----- */
    // The private MutableLiveData can be changed only within this ViewModel.
    private final MutableLiveData<SudokuBoard> _sudokuBoard = new MutableLiveData<>();
    private final MutableLiveData<Pair<Integer, Integer>> _selectedCell = new MutableLiveData<>();
    private final MutableLiveData<Long> _elapsedTimeInMillis = new MutableLiveData<>(0L);
    private final MutableLiveData<Integer> _errorCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> _score = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> _isGameWon = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isGameOverWithIncorrectBoard = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isGenerating = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> _generationErrorMessage = new MutableLiveData<>();
    // Tracks cumulative mistakes committed in the current game. Undo restores the board state,
    // but it does not erase mistakes that were already made.
    private int totalErrorsThisGame = 0;
    private boolean completionBonusApplied = false;
    private int awardedCompletionBonus = 0;
    private boolean winStatsRecorded = false;

    /* ----- Timer related fields ----- */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private long chronometerBase = 0L;
    private boolean isTimerRunning = false;

    // Executor for background tasks
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> generationTask;
    private volatile int generationRequestId = 0;

    /**
     * The public, immutable LiveData that the UI can observe. This follows the recommended pattern of exposing
     * only read-only LiveData to observers.
     */
    public LiveData<SudokuBoard> getSudokuBoard() {
        return _sudokuBoard;
    }

    public LiveData<Pair<Integer, Integer>> getSelectedCell() {
        return _selectedCell;
    }

    public LiveData<Long> getElapsedTimeInMillis() {
        return _elapsedTimeInMillis;
    }

    public LiveData<Integer> getErrorCount() {
        return _errorCount;
    }

    public LiveData<Integer> getScore() {
        return _score;
    }

    public LiveData<Boolean> isGameWon() {
        return _isGameWon;
    }

    public LiveData<Boolean> isGameOverWithIncorrectBoard() {
        return _isGameOverWithIncorrectBoard;
    }

    public LiveData<Boolean> isGenerating() {
        return _isGenerating;
    }

    public LiveData<Integer> getGenerationErrorMessage() {
        return _generationErrorMessage;
    }

    /**
     * Constructor for the ViewModel. Corresponds to the `init` block in Kotlin. It starts a new game upon
     * creation.
     */
    public SudokuViewModel() {
        // startNewGame(SudokuBoard.Difficulty.MEDIUM);
    }

    /**
     * Starts a new Sudoku game with the specified difficulty.
     *
     * @param difficulty The desired difficulty level.
     */
    public void startNewGame(SudokuBoard.Difficulty difficulty) {
        // Cancel any previously running generation task.
        if (generationTask != null && !generationTask.isDone()) {
            generationTask.cancel(true); // Pass true to interrupt the thread.
        }

        int requestId = ++generationRequestId;
        boolean shouldResumeTimerAfterFailure = isTimerRunning
                && _sudokuBoard.getValue() != null
                && !Boolean.TRUE.equals(_isGameWon.getValue())
                && !Boolean.TRUE.equals(_isGameOverWithIncorrectBoard.getValue());
        stopTimer();
        _generationErrorMessage.setValue(null);
        _isGenerating.setValue(true);
        generationTask = executor.submit(() -> {
            try {
                SudokuBoard newBoard = createBoardForGeneration();
                newBoard.generateNewPuzzle(difficulty);

                // If the task was cancelled, don't update the UI.
                if (Thread.currentThread().isInterrupted() || requestId != generationRequestId) {
                    return;
                }

                // Post the result back to the main thread
                mainHandler.post(() -> {
                    if (requestId != generationRequestId) {
                        return;
                    }
                    finishNewGameGeneration(newBoard);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (requestId == generationRequestId) {
                    mainHandler.post(() -> {
                        if (requestId == generationRequestId) {
                            if (shouldResumeTimerAfterFailure) {
                                startTimerIfNotRunning();
                            }
                            _isGenerating.setValue(false);
                        }
                    });
                }
            } catch (Exception e) {
                if (requestId == generationRequestId) {
                    mainHandler.post(() -> {
                        if (requestId == generationRequestId) {
                            if (shouldResumeTimerAfterFailure) {
                                startTimerIfNotRunning();
                            }
                            _isGenerating.setValue(false);
                            _generationErrorMessage.setValue(R.string.puzzle_generation_failed);
                        }
                    });
                }
            }
        });
    }

    /**
     * Selects a cell on the grid.
     *
     * @param row The row of the selected cell (0-8).
     * @param col The column of the selected cell (0-8).
     */
    public void selectCell(int row, int col) {
        _selectedCell.setValue(new Pair<>(row, col));
    }

    /**
     * Inputs a number into the currently selected cell. If the move is invalid, it increments the error count.
     * Updates the UI and checks the game status.
     *
     * @param number The number to input (1-9). If 0, the cell is cleared.
     */
    public void inputNumber(int number) {
        SudokuBoard board = _sudokuBoard.getValue();
        Pair<Integer, Integer> selection = _selectedCell.getValue();

        if (board == null || selection == null)
            return;

        int row = selection.first;
        int col = selection.second;
        SudokuCell cell = board.getCell(row, col);

        if (cell != null && !cell.isFixed()) {
            int oldValue = cell.getValue();
            if (oldValue == number) {
                return; // No change, do nothing
            }

            boolean isError = false;
            int scoreChange = 0;
            int currentScore = removeCompletionBonusFromScoreIfApplied();

            if (number != 0) {
                if (board.isMoveCorrect(row, col, number)) {
                    scoreChange = switch (board.getCurrentDifficulty()) {
                    case EASY -> 10;
                    case MEDIUM -> 15;
                    case HARD -> 20;
                    };
                } else {
                    isError = true;
                    this.totalErrorsThisGame++;
                    _errorCount.setValue(this.totalErrorsThisGame);
                    scoreChange = -50;
                }
            }

            // Calculate the actual score change, ensuring score doesn't go below zero
            int newScore = Math.max(0, currentScore + scoreChange);
            int actualScoreChange = newScore - currentScore;

            // Set the cell value and record the move with the actual score change and error status
            board.setCellValue(row, col, number, actualScoreChange, isError);

            // Update the score
            _score.setValue(newScore);

            _sudokuBoard.setValue(board); // Notify observers that the board has changed.
            checkGameStatus();
        }
    }

    /**
     * Undoes the last move.
     *
     * @return True if a move was undone, false otherwise.
     */
    public boolean undoLastMove() {
        SudokuBoard board = _sudokuBoard.getValue();
        if (board == null)
            return false;

        SudokuBoard.MoveRecord lastMove = board.undoMove();

        if (lastMove != null) {
            _sudokuBoard.setValue(board); // Notify observers

            // Revert the score change
            int currentScore = removeCompletionBonusFromScoreIfApplied();
            _score.setValue(Math.max(0, currentScore - lastMove.getScoreChange()));

            // The game might no longer be in a won/lost state after an undo.
            _isGameWon.setValue(false);
            _isGameOverWithIncorrectBoard.setValue(false);

            // If the timer was stopped because the game was over, restart it.
            if (!isTimerRunning && !board.isBoardFull()) {
                startTimerIfNotRunning();
            }
            return true;
        }
        return false;
    }

    /**
     * Clears the currently selected editable cell. If the cell contains a value, the operation follows the same path as
     * numeric input with {@code 0}; if it only contains notes, the notes are removed in place.
     *
     * @return {@code true} if something was cleared, {@code false} otherwise.
     */
    public boolean clearSelectedCell() {
        SudokuBoard board = _sudokuBoard.getValue();
        Pair<Integer, Integer> selection = _selectedCell.getValue();
        if (board == null || selection == null) {
            return false;
        }

        SudokuCell cell = board.getCell(selection.first, selection.second);
        if (cell == null || cell.isFixed()) {
            return false;
        }

        if (cell.getValue() != 0) {
            inputNumber(0);
            return true;
        }

        if (!cell.getNotes().isEmpty()) {
            cell.clearNotes();
            _sudokuBoard.setValue(board);
            return true;
        }

        return false;
    }

    /**
     * Marks win statistics as recorded for the current puzzle.
     *
     * @return {@code true} the first time the win is recorded, {@code false} on subsequent calls.
     */
    public boolean markWinStatsRecordedIfNeeded() {
        if (winStatsRecorded) {
            return false;
        }
        winStatsRecorded = true;
        return true;
    }

    public void clearGenerationErrorMessage() {
        _generationErrorMessage.setValue(null);
    }

    /**
     * Saves the current state of the ViewModel. Used for onSaveInstanceState in the Activity.
     *
     * @return A Pair containing the SudokuBoard state and a Bundle with other state information.
     */
    public Pair<SudokuBoard, Bundle> saveState() {
        Bundle bundle = new Bundle();
        Pair<Integer, Integer> selection = _selectedCell.getValue();
        if (selection != null) {
            bundle.putInt(STATE_SELECTED_ROW, selection.first);
            bundle.putInt(STATE_SELECTED_COL, selection.second);
        }
        bundle.putLong(STATE_CHRONOMETER_BASE, chronometerBase);
        bundle.putBoolean(STATE_IS_TIMER_RUNNING, isTimerRunning);
        bundle.putLong(STATE_ELAPSED_TIME_IN_MILLIS, Objects.requireNonNullElse(_elapsedTimeInMillis.getValue(), 0L));
        bundle.putInt(STATE_ERROR_COUNT, Objects.requireNonNullElse(_errorCount.getValue(), 0));
        bundle.putInt(STATE_TOTAL_ERRORS, totalErrorsThisGame);
        bundle.putInt(STATE_SCORE, Objects.requireNonNullElse(_score.getValue(), 0));
        bundle.putBoolean(STATE_IS_GAME_WON, Boolean.TRUE.equals(_isGameWon.getValue()));
        bundle.putBoolean(STATE_IS_GAME_OVER_WITH_INCORRECT_BOARD,
                Boolean.TRUE.equals(_isGameOverWithIncorrectBoard.getValue()));
        bundle.putBoolean(STATE_COMPLETION_BONUS_APPLIED, completionBonusApplied);
        bundle.putInt(STATE_AWARDED_COMPLETION_BONUS, awardedCompletionBonus);
        bundle.putBoolean(STATE_WIN_STATS_RECORDED, winStatsRecorded);

        return new Pair<>(_sudokuBoard.getValue(), bundle);
    }

    /**
     * Restores the state of the ViewModel. Used from onCreate or onRestoreInstanceState in the Activity.
     *
     * @param boardState The SudokuBoard state to restore.
     * @param bundleState Bundle containing other state information.
     */
    public void restoreState(SudokuBoard boardState, Bundle bundleState) {
        if (boardState != null) {
            _sudokuBoard.setValue(boardState);
        }
        _isGenerating.setValue(false);

        int savedSelectedRow = bundleState.getInt(STATE_SELECTED_ROW, -1);
        int savedSelectedCol = bundleState.getInt(STATE_SELECTED_COL, -1);
        if (savedSelectedRow != -1 && savedSelectedCol != -1) {
            _selectedCell.setValue(new Pair<>(savedSelectedRow, savedSelectedCol));
        } else {
            _selectedCell.setValue(null);
        }

        long restoredElapsedTime = bundleState.getLong(STATE_ELAPSED_TIME_IN_MILLIS, 0L);
        _elapsedTimeInMillis.setValue(restoredElapsedTime);
        // Resume from the elapsed time snapshot so reopening the app does not count time spent closed.
        chronometerBase = SystemClock.elapsedRealtime() - restoredElapsedTime;
        boolean shouldResumeTimer = bundleState.getBoolean(STATE_IS_TIMER_RUNNING, false);
        isTimerRunning = false;
        totalErrorsThisGame = bundleState.getInt(STATE_TOTAL_ERRORS,
                bundleState.getInt(STATE_ERROR_COUNT,
                        _sudokuBoard.getValue() != null ? _sudokuBoard.getValue().countUserErrors() : 0));
        _errorCount.setValue(totalErrorsThisGame);
        _score.setValue(bundleState.getInt(STATE_SCORE, 0));
        completionBonusApplied = bundleState.getBoolean(STATE_COMPLETION_BONUS_APPLIED, false);
        awardedCompletionBonus = bundleState.getInt(STATE_AWARDED_COMPLETION_BONUS, 0);
        winStatsRecorded = bundleState.getBoolean(STATE_WIN_STATS_RECORDED, false);

        boolean restoredGameWon = bundleState.getBoolean(STATE_IS_GAME_WON, false);
        boolean restoredIncorrectBoard = bundleState.getBoolean(STATE_IS_GAME_OVER_WITH_INCORRECT_BOARD, false);
        _isGameWon.setValue(restoredGameWon);
        _isGameOverWithIncorrectBoard.setValue(restoredIncorrectBoard);

        if (restoredGameWon || restoredIncorrectBoard) {
            stopTimer();
            return;
        }

        // If the game was running and not finished, restart the timer.
        if (_sudokuBoard.getValue() != null && _sudokuBoard.getValue().isBoardFull()) {
            stopTimer();
            checkGameStatus();
        } else if (shouldResumeTimer) {
            startTimerIfNotRunning();
        } else {
            stopTimer();
        }
    }

    /* ----- Private Helper Methods ----- */

    protected SudokuBoard createBoardForGeneration() {
        return new SudokuBoard();
    }

    private void startTimerIfNotRunning() {
        if (!isTimerRunning) {
            if (chronometerBase == 0L) {
                // On first start or after a reset
                chronometerBase = SystemClock.elapsedRealtime()
                        - Objects.requireNonNullElse(_elapsedTimeInMillis.getValue(), 0L);
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
     * Checks if the game is over (either won or the board is full but incorrect). Updates the relevant LiveData
     * for the UI to observe.
     */
    private void checkGameStatus() {
        SudokuBoard board = _sudokuBoard.getValue();
        if (board == null)
            return;

        if (board.isBoardFull()) {
            stopTimer();

            // Check if the board is valid and all user cells are correct
            if (board.isCurrentBoardStateValidAccordingToRules() && board.areAllUserCellsCorrect()) {
                // Game won - calculate final score with bonus
                if (!completionBonusApplied) {
                    long timeInMillis = Objects.requireNonNullElse(_elapsedTimeInMillis.getValue(), 0L);
                    awardedCompletionBonus = calculateCompletionBonus(board, timeInMillis);
                    int currentScore = Objects.requireNonNullElse(_score.getValue(), 0);
                    _score.setValue(currentScore + awardedCompletionBonus);
                    completionBonusApplied = true;
                }
                _isGameOverWithIncorrectBoard.setValue(false);
                _isGameWon.setValue(true);
            } else {
                // Board is full but incorrect
                _isGameWon.setValue(false);
                _isGameOverWithIncorrectBoard.setValue(true);
            }
        } else {
            _isGameWon.setValue(false);
            _isGameOverWithIncorrectBoard.setValue(false);
            startTimerIfNotRunning();
        }
    }

    private void finishNewGameGeneration(SudokuBoard newBoard) {
        resetForNewGameRequest();
        _sudokuBoard.setValue(newBoard);
        _selectedCell.setValue(null);
        _isGenerating.setValue(false);
        resetAndStartTimer();
    }

    private void resetForNewGameRequest() {
        _selectedCell.setValue(null);
        _elapsedTimeInMillis.setValue(0L);
        _errorCount.setValue(0);
        _score.setValue(0);
        totalErrorsThisGame = 0;
        completionBonusApplied = false;
        awardedCompletionBonus = 0;
        winStatsRecorded = false;
        _isGameWon.setValue(false);
        _isGameOverWithIncorrectBoard.setValue(false);
        chronometerBase = 0L;
    }

    private int removeCompletionBonusFromScoreIfApplied() {
        int currentScore = Objects.requireNonNullElse(_score.getValue(), 0);
        if (!completionBonusApplied) {
            return currentScore;
        }

        currentScore = Math.max(0, currentScore - awardedCompletionBonus);
        completionBonusApplied = false;
        awardedCompletionBonus = 0;
        _score.setValue(currentScore);
        _isGameWon.setValue(false);
        _isGameOverWithIncorrectBoard.setValue(false);
        return currentScore;
    }

    private int calculateCompletionBonus(SudokuBoard board, long timeInMillis) {
        long timeBonus = Math.max(0, 600 - (timeInMillis / 1000)); // Example bonus: 600 seconds base

        int difficultyBonus = switch (board.getCurrentDifficulty()) {
        case EASY -> 500;
        case HARD -> 2000;
        default -> 1000;
        };

        return (int) (timeBonus + difficultyBonus);
    }

    /**
     * This method is called when the ViewModel is about to be destroyed. It's the perfect place to clean up
     * resources, like stopping the timer handler.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        stopTimer(); // Stop the handler callbacks to prevent memory leaks.
        if (generationTask != null && !generationTask.isDone()) {
            generationTask.cancel(true);
        }
        executor.shutdown();
    }
}
