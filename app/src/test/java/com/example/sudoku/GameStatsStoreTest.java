package com.example.sudoku;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GameStatsStoreTest {

    private final Context context = RuntimeEnvironment.getApplication();

    @Before
    @After
    public void clearStore() {
        GameStatsStore.clear(context);
    }

    @Test
    public void recordWin_updatesWinsBestTimeAndBestScorePerDifficulty() {
        GameStatsStore.recordWin(context, SudokuBoard.Difficulty.EASY, 300_000L, 1200);
        GameStatsStore.recordWin(context, SudokuBoard.Difficulty.EASY, 360_000L, 1100);
        GameStatsStore.recordWin(context, SudokuBoard.Difficulty.EASY, 240_000L, 1300);
        GameStatsStore.recordWin(context, SudokuBoard.Difficulty.HARD, 600_000L, 2200);

        GameStatsStore.StatsSnapshot snapshot = GameStatsStore.load(context);
        GameStatsStore.DifficultyStats easyStats = snapshot.getStats(SudokuBoard.Difficulty.EASY);
        GameStatsStore.DifficultyStats hardStats = snapshot.getStats(SudokuBoard.Difficulty.HARD);

        assertTrue(snapshot.hasAnyStats());
        assertEquals(3, easyStats.getWins());
        assertEquals(240_000L, easyStats.getBestTimeInMillis());
        assertEquals(1300, easyStats.getBestScore());
        assertEquals(1, hardStats.getWins());
        assertEquals(600_000L, hardStats.getBestTimeInMillis());
        assertEquals(2200, hardStats.getBestScore());
    }
}
