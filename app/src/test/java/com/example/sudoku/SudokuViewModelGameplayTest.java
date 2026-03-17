package com.example.sudoku;

import android.os.Bundle;
import android.os.Looper;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.sudoku.viewmodel.SudokuViewModel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Gameplay-focused tests for {@link SudokuViewModel}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SudokuViewModelGameplayTest {

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
    private static final String STATE_IS_PAUSED = "isPaused";
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

    /**
     * Correct moves should award difficulty-specific points without ending the game early.
     */
    @Test
    public void correctMove_awardsPointsByDifficultyWithoutTriggeringEndgame() throws Exception {
        assertScoreForCorrectMove(SudokuBoard.Difficulty.EASY, 10);
        assertScoreForCorrectMove(SudokuBoard.Difficulty.MEDIUM, 15);
        assertScoreForCorrectMove(SudokuBoard.Difficulty.HARD, 20);
    }

    /**
     * Undo must restore board/score state but keep historical errors accounted for.
     */
    @Test
    public void incorrectMoveAndUndo_restoresBoardButKeepsHistoricalErrorCount() throws Exception {
        SudokuViewModel viewModel = new SudokuViewModel();
        viewModel.restoreState(createBoardWithOpenCells(SudokuBoard.Difficulty.MEDIUM, new int[][] { { 0, 0 }, { 0, 1 } }),
                createBundle(0, 0, 40));

        viewModel.inputNumber(9);

        assertEquals(Integer.valueOf(0), viewModel.getScore().getValue());
        assertEquals(Integer.valueOf(1), viewModel.getErrorCount().getValue());
        assertFalse(viewModel.getSudokuBoard().getValue().getCell(0, 0).isCorrect());

        assertTrue(viewModel.undoLastMove());
        assertEquals(Integer.valueOf(40), viewModel.getScore().getValue());
        assertEquals(Integer.valueOf(1), viewModel.getErrorCount().getValue());
        assertEquals(0, viewModel.getSudokuBoard().getValue().getCell(0, 0).getValue());
    }

    /**
     * Clearing an editable value should remove the number while preserving the existing score.
     */
    @Test
    public void clearSelectedCell_clearsEditableValueWithoutChangingScore() throws Exception {
        SudokuViewModel viewModel = new SudokuViewModel();
        viewModel.restoreState(createBoardWithOpenCells(SudokuBoard.Difficulty.EASY, new int[][] { { 0, 0 }, { 0, 1 } }),
                createBundle(0, 0, 0));

        viewModel.inputNumber(SOLUTION[0][0]);

        assertEquals(Integer.valueOf(10), viewModel.getScore().getValue());
        assertEquals(SOLUTION[0][0], viewModel.getSudokuBoard().getValue().getCell(0, 0).getValue());

        assertTrue(viewModel.clearSelectedCell());
        assertEquals(Integer.valueOf(10), viewModel.getScore().getValue());
        assertEquals(0, viewModel.getSudokuBoard().getValue().getCell(0, 0).getValue());
        assertTrue(viewModel.getSudokuBoard().getValue().getCell(0, 0).isCorrect());
    }

    /**
     * Pausing must freeze gameplay input until the player explicitly resumes.
     */
    @Test
    public void pauseBlocksGameplayInputUntilResumed() throws Exception {
        SudokuViewModel viewModel = new SudokuViewModel();
        viewModel.restoreState(createBoardWithOpenCells(SudokuBoard.Difficulty.MEDIUM, new int[][] { { 0, 0 }, { 0, 1 } }),
                createBundle(0, 0, 0));

        assertTrue(viewModel.togglePause());
        assertEquals(Boolean.TRUE, viewModel.isPaused().getValue());

        viewModel.inputNumber(SOLUTION[0][0]);

        assertEquals(0, viewModel.getSudokuBoard().getValue().getCell(0, 0).getValue());
        assertFalse(viewModel.clearSelectedCell());
        assertFalse(viewModel.undoLastMove());

        assertTrue(viewModel.togglePause());
        assertEquals(Boolean.FALSE, viewModel.isPaused().getValue());

        viewModel.inputNumber(SOLUTION[0][0]);

        assertEquals(SOLUTION[0][0], viewModel.getSudokuBoard().getValue().getCell(0, 0).getValue());
        assertEquals(Integer.valueOf(15), viewModel.getScore().getValue());
    }

    /**
     * When multiple generations are requested quickly, only the latest request may update state.
     */
    @Test(timeout = 30000)
    public void startNewGameInQuickSuccession_keepsLatestDifficulty() throws Exception {
        SudokuViewModel viewModel = new SudokuViewModel();

        viewModel.startNewGame(SudokuBoard.Difficulty.EASY);
        viewModel.startNewGame(SudokuBoard.Difficulty.HARD);

        long deadline = System.currentTimeMillis() + 25000;
        while (System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            SudokuBoard board = viewModel.getSudokuBoard().getValue();
            Boolean isGenerating = viewModel.isGenerating().getValue();
            if (board != null && Boolean.FALSE.equals(isGenerating)) {
                assertEquals(SudokuBoard.Difficulty.HARD, board.getCurrentDifficulty());
                return;
            }

            Thread.sleep(25);
        }

        fail("Timed out waiting for the latest generated puzzle.");
    }

    /**
     * Generation failures should surface an error and keep the previous playable game intact.
     */
    @Test(timeout = 10000)
    public void startNewGameFailure_keepsCurrentGameAndExposesError() throws Exception {
        SudokuBoard existingBoard = createBoardWithOpenCells(SudokuBoard.Difficulty.MEDIUM, new int[][] { { 0, 0 }, { 0, 1 } });
        SudokuViewModel viewModel = new FailingGenerationSudokuViewModel();
        viewModel.restoreState(existingBoard, createBundle(0, 1, 35));

        viewModel.startNewGame(SudokuBoard.Difficulty.HARD);

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            if (Boolean.FALSE.equals(viewModel.isGenerating().getValue())
                    && viewModel.getGenerationErrorMessage().getValue() != null) {
                assertSame(existingBoard, viewModel.getSudokuBoard().getValue());
                assertEquals(Integer.valueOf(35), viewModel.getScore().getValue());
                assertEquals(Integer.valueOf(R.string.puzzle_generation_failed), viewModel.getGenerationErrorMessage().getValue());
                assertEquals(Integer.valueOf(0), viewModel.getSelectedCell().getValue().first);
                assertEquals(Integer.valueOf(1), viewModel.getSelectedCell().getValue().second);
                return;
            }

            Thread.sleep(25);
        }

        fail("Timed out waiting for the generation failure state.");
    }

    private void assertScoreForCorrectMove(SudokuBoard.Difficulty difficulty, int expectedScore) throws Exception {
        SudokuViewModel viewModel = new SudokuViewModel();
        viewModel.restoreState(createBoardWithOpenCells(difficulty, new int[][] { { 0, 0 }, { 0, 1 } }),
                createBundle(0, 0, 0));

        viewModel.inputNumber(SOLUTION[0][0]);

        assertEquals(Integer.valueOf(expectedScore), viewModel.getScore().getValue());
        assertEquals(Integer.valueOf(0), viewModel.getErrorCount().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.isGameWon().getValue()));
    }

    /**
     * Builds a deterministic test board with selected editable cells.
     */
    private SudokuBoard createBoardWithOpenCells(SudokuBoard.Difficulty difficulty, int[][] openCells) throws Exception {
        SudokuBoard board = new SudokuBoard();
        SudokuCell[][] cells = new SudokuCell[9][9];
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                cells[row][col] = new SudokuCell(SOLUTION[row][col], true, true, null);
            }
        }
        for (int[] openCell : openCells) {
            cells[openCell[0]][openCell[1]] = new SudokuCell(0, false, true, null);
        }

        setField(board, "currentDifficulty", difficulty);
        setField(board, "solutionBoard", copySolution());
        setField(board, "board", cells);
        return board;
    }

    /**
     * Creates a deep copy of the fixture solution matrix.
     */
    private int[][] copySolution() {
        int[][] copy = new int[9][9];
        for (int row = 0; row < 9; row++) {
            System.arraycopy(SOLUTION[row], 0, copy[row], 0, 9);
        }
        return copy;
    }

    /**
     * Creates a state bundle compatible with {@link SudokuViewModel#restoreState(SudokuBoard, Bundle)}.
     */
    private Bundle createBundle(int selectedRow, int selectedCol, int score) {
        Bundle bundle = new Bundle();
        bundle.putInt(STATE_SELECTED_ROW, selectedRow);
        bundle.putInt(STATE_SELECTED_COL, selectedCol);
        bundle.putLong(STATE_CHRONOMETER_BASE, 0L);
        bundle.putBoolean(STATE_IS_TIMER_RUNNING, false);
        bundle.putLong(STATE_ELAPSED_TIME_IN_MILLIS, 0L);
        bundle.putInt(STATE_ERROR_COUNT, 0);
        bundle.putInt(STATE_TOTAL_ERRORS, 0);
        bundle.putInt(STATE_SCORE, score);
        bundle.putBoolean(STATE_IS_GAME_WON, false);
        bundle.putBoolean(STATE_IS_GAME_OVER_WITH_INCORRECT_BOARD, false);
        bundle.putBoolean(STATE_IS_PAUSED, false);
        bundle.putBoolean(STATE_COMPLETION_BONUS_APPLIED, false);
        bundle.putInt(STATE_AWARDED_COMPLETION_BONUS, 0);
        return bundle;
    }

    /**
     * Reflection helper used by tests to inject deterministic board internals.
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Test double that forces puzzle generation to fail in background execution.
     */
    private static final class FailingGenerationSudokuViewModel extends SudokuViewModel {
        @Override
        protected SudokuBoard createBoardForGeneration() {
            return new SudokuBoard() {
                @Override
                public void generateNewPuzzle(Difficulty difficulty) {
                    throw new IllegalStateException("Synthetic generation failure");
                }
            };
        }
    }
}
