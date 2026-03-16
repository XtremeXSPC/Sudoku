package com.example.sudoku;

import android.content.Context;
import android.os.Bundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link SavedGameStore} persistence and key isolation behavior.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SavedGameStoreTest {

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

    private final Context context = RuntimeEnvironment.getApplication();

    /**
     * Resets shared preferences touched by this suite.
     */
    @Before
    @After
    public void clearStore() {
        context.getSharedPreferences("saved_game_preferences", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    /**
     * Saves and reloads a board plus ViewModel payload, including notes and correctness flags.
     */
    @Test
    public void saveAndLoad_roundTripsBoardAndViewModelState() throws Exception {
        SudokuBoard board = createBoardWithNotesAndError();
        Bundle bundle = new Bundle();
        bundle.putInt("selectedRow", 0);
        bundle.putInt("selectedCol", 1);
        bundle.putInt("score", 450);
        bundle.putLong("elapsedTimeInMillis", 123_000L);

        SavedGameStore.save(context, board, bundle);

        assertTrue(SavedGameStore.hasSavedGame(context));

        SavedGameStore.SavedGame loadedGame = SavedGameStore.load(context);
        assertNotNull(loadedGame);
        assertEquals(SudokuBoard.Difficulty.HARD, loadedGame.getBoard().getCurrentDifficulty());
        assertEquals(0, loadedGame.getBoard().getCell(0, 0).getValue());
        assertEquals(3, loadedGame.getBoard().getCell(0, 0).getNotes().size());
        assertFalse(loadedGame.getBoard().getCell(0, 1).isCorrect());
        assertEquals(1, loadedGame.getViewModelState().getInt("selectedCol"));
        assertEquals(450, loadedGame.getViewModelState().getInt("score"));
    }

    /**
     * Verifies that explicit clear removes the saved game payload.
     */
    @Test
    public void clear_removesSavedGame() throws Exception {
        SavedGameStore.save(context, createBoardWithNotesAndError(), new Bundle());
        assertTrue(SavedGameStore.hasSavedGame(context));

        SavedGameStore.clear(context);

        assertFalse(SavedGameStore.hasSavedGame(context));
        assertNull(SavedGameStore.load(context));
    }

    /**
     * Verifies that {@link SavedGameStore#clear(Context)} does not wipe unrelated preference keys.
     */
    @Test
    public void clear_preservesUnrelatedPreferencesEntries() throws Exception {
        context.getSharedPreferences("saved_game_preferences", Context.MODE_PRIVATE)
                .edit()
                .putString("theme", "sepia")
                .apply();
        SavedGameStore.save(context, createBoardWithNotesAndError(), new Bundle());

        SavedGameStore.clear(context);

        assertEquals("sepia", context.getSharedPreferences("saved_game_preferences", Context.MODE_PRIVATE)
                .getString("theme", null));
    }

    private SudokuBoard createBoardWithNotesAndError() throws Exception {
        SudokuBoard board = new SudokuBoard();
        SudokuCell[][] cells = new SudokuCell[9][9];
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                cells[row][col] = new SudokuCell(SOLUTION[row][col], true, true, null);
            }
        }

        cells[0][0] = new SudokuCell(0, false, true, new HashSet<>(Arrays.asList(1, 2, 3)));
        cells[0][1] = new SudokuCell(9, false, false, null);

        setField(board, "currentDifficulty", SudokuBoard.Difficulty.HARD);
        setField(board, "solutionBoard", copySolution());
        setField(board, "board", cells);
        return board;
    }

    private int[][] copySolution() {
        int[][] copy = new int[9][9];
        for (int row = 0; row < 9; row++) {
            System.arraycopy(SOLUTION[row], 0, copy[row], 0, 9);
        }
        return copy;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
