package com.example.sudoku;

import android.os.Bundle;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.core.util.Pair;

import com.example.sudoku.viewmodel.SudokuViewModel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.Stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SudokuViewModelRestoreTest {

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

    private static final int[][] SOLUTION = new int[][] {
            { 5, 3, 4, 6, 7, 8, 9, 1, 2 },
            { 6, 7, 2, 1, 9, 5, 3, 4, 8 },
            { 1, 9, 8, 3, 4, 2, 5, 6, 7 },
            { 8, 5, 9, 7, 6, 1, 4, 2, 3 },
            { 4, 2, 6, 8, 5, 3, 7, 9, 1 },
            { 7, 1, 3, 9, 2, 4, 8, 5, 6 },
            { 9, 6, 1, 5, 3, 7, 2, 8, 4 },
            { 2, 8, 7, 4, 1, 9, 6, 3, 5 },
            { 3, 4, 5, 2, 8, 6, 1, 7, 9 }
    };

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void saveAndRestore_preservesTotalErrorsForFutureMoves() throws Exception {
        SudokuViewModel originalViewModel = new SudokuViewModel();
        originalViewModel.restoreState(createBoardWithOnePastErrorAndOneOpenCell(),
                createBundle(0, 1, 1, 1, 0, false, false, false, false, 0));

        Pair<SudokuBoard, Bundle> savedState = originalViewModel.saveState();

        SudokuViewModel restoredViewModel = new SudokuViewModel();
        restoredViewModel.restoreState(savedState.first, savedState.second);
        restoredViewModel.inputNumber(8);

        assertEquals(Integer.valueOf(2), restoredViewModel.getErrorCount().getValue());
        assertEquals(Integer.valueOf(0), restoredViewModel.getScore().getValue());
        assertEquals(Boolean.TRUE, restoredViewModel.isGameOverWithIncorrectBoard().getValue());
    }

    @Test
    public void saveAndRestore_undoPreservesHistoricalErrorCount() throws Exception {
        SudokuViewModel originalViewModel = new SudokuViewModel();
        originalViewModel.restoreState(createBoardWithOnePastErrorAndOneOpenCell(),
                createBundle(0, 1, 1, 1, 0, false, false, false, false, 0));

        Pair<SudokuBoard, Bundle> savedState = originalViewModel.saveState();

        SudokuViewModel restoredViewModel = new SudokuViewModel();
        restoredViewModel.restoreState(savedState.first, savedState.second);

        assertTrue(restoredViewModel.undoLastMove());
        assertEquals(Integer.valueOf(1), restoredViewModel.getErrorCount().getValue());
        assertEquals(0, restoredViewModel.getSudokuBoard().getValue().getCell(0, 0).getValue());
    }

    @Test
    public void saveAndRestore_doesNotAwardCompletionBonusTwice() throws Exception {
        SudokuViewModel originalViewModel = new SudokuViewModel();
        originalViewModel.restoreState(createWonBoardWithWinningMove(),
                createBundle(-1, -1, 0, 0, 1210, false, true, false, true, 1200));

        Pair<SudokuBoard, Bundle> savedState = originalViewModel.saveState();

        SudokuViewModel restoredViewModel = new SudokuViewModel();
        restoredViewModel.restoreState(savedState.first, savedState.second);

        assertEquals(Integer.valueOf(1210), restoredViewModel.getScore().getValue());
        assertEquals(Boolean.TRUE, restoredViewModel.isGameWon().getValue());
        assertEquals(Boolean.FALSE, restoredViewModel.isGameOverWithIncorrectBoard().getValue());
    }

    @Test
    public void undoAfterRestoredWin_removesCompletionBonusBeforeRevertingTheWinningMove() throws Exception {
        SudokuViewModel originalViewModel = new SudokuViewModel();
        originalViewModel.restoreState(createWonBoardWithWinningMove(),
                createBundle(-1, -1, 0, 0, 1210, false, true, false, true, 1200));

        Pair<SudokuBoard, Bundle> savedState = originalViewModel.saveState();

        SudokuViewModel restoredViewModel = new SudokuViewModel();
        restoredViewModel.restoreState(savedState.first, savedState.second);

        assertTrue(restoredViewModel.undoLastMove());
        assertEquals(Integer.valueOf(0), restoredViewModel.getScore().getValue());
        assertFalse(Boolean.TRUE.equals(restoredViewModel.isGameWon().getValue()));
        assertEquals(0, restoredViewModel.getSudokuBoard().getValue().getCell(0, 0).getValue());
    }

    private SudokuBoard createBoardWithOnePastErrorAndOneOpenCell() throws Exception {
        SudokuBoard board = new SudokuBoard();
        SudokuCell[][] cells = createFixedSolvedBoard();
        cells[0][0] = new SudokuCell(9, false, false, null);
        cells[0][1] = new SudokuCell(0, false, true, null);

        configureBoard(board, cells);
        getMoveHistory(board).push(new SudokuBoard.MoveRecord(0, 0, 0, 9, 0, true));
        return board;
    }

    private SudokuBoard createWonBoardWithWinningMove() throws Exception {
        SudokuBoard board = new SudokuBoard();
        SudokuCell[][] cells = createFixedSolvedBoard();
        cells[0][0] = new SudokuCell(SOLUTION[0][0], false, true, null);

        configureBoard(board, cells);
        getMoveHistory(board).push(new SudokuBoard.MoveRecord(0, 0, 0, SOLUTION[0][0], 10, false));
        return board;
    }

    private void configureBoard(SudokuBoard board, SudokuCell[][] cells) throws Exception {
        setField(board, "currentDifficulty", SudokuBoard.Difficulty.EASY);
        setField(board, "solutionBoard", copySolution());
        setField(board, "board", cells);
        getMoveHistory(board).clear();
    }

    private SudokuCell[][] createFixedSolvedBoard() {
        SudokuCell[][] cells = new SudokuCell[9][9];
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                cells[row][col] = new SudokuCell(SOLUTION[row][col], true, true, null);
            }
        }
        return cells;
    }

    private int[][] copySolution() {
        int[][] copy = new int[9][9];
        for (int row = 0; row < 9; row++) {
            System.arraycopy(SOLUTION[row], 0, copy[row], 0, 9);
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private Stack<SudokuBoard.MoveRecord> getMoveHistory(SudokuBoard board) throws Exception {
        Field movesHistoryField = SudokuBoard.class.getDeclaredField("movesHistory");
        movesHistoryField.setAccessible(true);
        return (Stack<SudokuBoard.MoveRecord>) movesHistoryField.get(board);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Bundle createBundle(int selectedRow, int selectedCol, int errorCount, int totalErrors, int score,
            boolean isTimerRunning, boolean isGameWon, boolean isIncorrectBoard, boolean completionBonusApplied,
            int awardedCompletionBonus) {
        Bundle bundle = new Bundle();
        if (selectedRow >= 0 && selectedCol >= 0) {
            bundle.putInt(STATE_SELECTED_ROW, selectedRow);
            bundle.putInt(STATE_SELECTED_COL, selectedCol);
        }
        bundle.putLong(STATE_CHRONOMETER_BASE, 0L);
        bundle.putBoolean(STATE_IS_TIMER_RUNNING, isTimerRunning);
        bundle.putLong(STATE_ELAPSED_TIME_IN_MILLIS, 0L);
        bundle.putInt(STATE_ERROR_COUNT, errorCount);
        bundle.putInt(STATE_TOTAL_ERRORS, totalErrors);
        bundle.putInt(STATE_SCORE, score);
        bundle.putBoolean(STATE_IS_GAME_WON, isGameWon);
        bundle.putBoolean(STATE_IS_GAME_OVER_WITH_INCORRECT_BOARD, isIncorrectBoard);
        bundle.putBoolean(STATE_COMPLETION_BONUS_APPLIED, completionBonusApplied);
        bundle.putInt(STATE_AWARDED_COMPLETION_BONUS, awardedCompletionBonus);
        return bundle;
    }
}
