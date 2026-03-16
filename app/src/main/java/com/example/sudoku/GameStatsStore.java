package com.example.sudoku;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Stores lightweight local statistics used by the dedicated statistics screen and release-ready polish.
 */
public final class GameStatsStore {

    private static final String PREFS_NAME = "game_stats_preferences";

    private GameStatsStore() {
    }

    public static void recordWin(@NonNull Context context, @NonNull SudokuBoard.Difficulty difficulty, long elapsedTimeInMillis,
            int score) {
        SharedPreferences preferences = getPreferences(context);
        DifficultyStats currentStats  = readDifficultyStats(preferences, difficulty);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(winsKey(difficulty), currentStats.getWins() + 1);

        if (currentStats.getBestTimeInMillis() < 0 || elapsedTimeInMillis < currentStats.getBestTimeInMillis()) {
            editor.putLong(bestTimeKey(difficulty), elapsedTimeInMillis);
        }

        if (currentStats.getBestScore() < 0 || score > currentStats.getBestScore()) {
            editor.putInt(bestScoreKey(difficulty), score);
        }

        editor.apply();
    }

    @NonNull
    public static StatsSnapshot load(@NonNull Context context) {
        SharedPreferences preferences = getPreferences(context);
        return new StatsSnapshot(
                readDifficultyStats(preferences, SudokuBoard.Difficulty.EASY),
                readDifficultyStats(preferences, SudokuBoard.Difficulty.MEDIUM),
                readDifficultyStats(preferences, SudokuBoard.Difficulty.HARD));
    }

    public static void clear(@NonNull Context context) {
        getPreferences(context).edit().clear().apply();
    }

    @NonNull
    public static StatsSnapshot emptySnapshot() {
        DifficultyStats emptyStats = new DifficultyStats(0, -1L, -1);
        return new StatsSnapshot(emptyStats, emptyStats, emptyStats);
    }

    @NonNull
    private static SharedPreferences getPreferences(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    private static DifficultyStats readDifficultyStats(@NonNull SharedPreferences preferences,
            @NonNull SudokuBoard.Difficulty difficulty) {
        return new DifficultyStats(
                preferences.getInt(winsKey(difficulty), 0),
                preferences.getLong(bestTimeKey(difficulty), -1L),
                preferences.getInt(bestScoreKey(difficulty), -1));
    }

    @NonNull
    private static String winsKey(@NonNull SudokuBoard.Difficulty difficulty) {
        return difficultyKey(difficulty) + "_wins";
    }

    @NonNull
    private static String bestTimeKey(@NonNull SudokuBoard.Difficulty difficulty) {
        return difficultyKey(difficulty) + "_best_time";
    }

    @NonNull
    private static String bestScoreKey(@NonNull SudokuBoard.Difficulty difficulty) {
        return difficultyKey(difficulty) + "_best_score";
    }

    @NonNull
    private static String difficultyKey(@NonNull SudokuBoard.Difficulty difficulty) {
        return difficulty.name().toLowerCase(Locale.US);
    }

    public static final class StatsSnapshot {
        private final DifficultyStats easyStats;
        private final DifficultyStats mediumStats;
        private final DifficultyStats hardStats;

        StatsSnapshot(@NonNull DifficultyStats easyStats, @NonNull DifficultyStats mediumStats,
                @NonNull DifficultyStats hardStats) {
            this.easyStats = easyStats;
            this.mediumStats = mediumStats;
            this.hardStats = hardStats;
        }

        @NonNull
        public DifficultyStats getStats(@NonNull SudokuBoard.Difficulty difficulty) {
            return switch (difficulty) {
            case EASY -> easyStats;
            case HARD -> hardStats;
            default -> mediumStats;
            };
        }

        public boolean hasAnyStats() {
            return easyStats.hasResults() || mediumStats.hasResults() || hardStats.hasResults();
        }
    }

    public static final class DifficultyStats {
        private final int wins;
        private final long bestTimeInMillis;
        private final int bestScore;

        DifficultyStats(int wins, long bestTimeInMillis, int bestScore) {
            this.wins = wins;
            this.bestTimeInMillis = bestTimeInMillis;
            this.bestScore = bestScore;
        }

        public int getWins() {
            return wins;
        }

        public long getBestTimeInMillis() {
            return bestTimeInMillis;
        }

        public int getBestScore() {
            return bestScore;
        }

        public boolean hasResults() {
            return wins > 0;
        }
    }
}
