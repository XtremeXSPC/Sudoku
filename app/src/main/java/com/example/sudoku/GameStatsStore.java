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

    /**
     * Records a completed game for the provided difficulty and updates best-time / best-score aggregates.
     *
     * @param context Android context used to access {@link SharedPreferences}.
     * @param difficulty Difficulty of the puzzle that has just been completed.
     * @param elapsedTimeInMillis Completion time in milliseconds.
     * @param score Final score reached for the completed game.
     */
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

    /**
     * Loads the persisted statistics snapshot for all difficulties.
     *
     * @param context Android context used to access {@link SharedPreferences}.
     * @return A full snapshot containing easy, medium and hard aggregates.
     */
    @NonNull
    public static StatsSnapshot load(@NonNull Context context) {
        SharedPreferences preferences = getPreferences(context);
        return new StatsSnapshot(
                readDifficultyStats(preferences, SudokuBoard.Difficulty.EASY),
                readDifficultyStats(preferences, SudokuBoard.Difficulty.MEDIUM),
                readDifficultyStats(preferences, SudokuBoard.Difficulty.HARD));
    }

    /**
     * Clears all persisted statistics managed by this store.
     *
     * @param context Android context used to access {@link SharedPreferences}.
     */
    public static void clear(@NonNull Context context) {
        getPreferences(context).edit().clear().apply();
    }

    /**
     * Returns an in-memory empty snapshot where every metric is set to its sentinel value.
     *
     * @return A snapshot with zero wins and unavailable best metrics.
     */
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

    /**
     * Read-only container with aggregated statistics for every difficulty level.
     */
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

        /**
         * Returns statistics for one difficulty.
         *
         * @param difficulty Difficulty to query.
         * @return Aggregated metrics for the requested difficulty.
         */
        @NonNull
        public DifficultyStats getStats(@NonNull SudokuBoard.Difficulty difficulty) {
            return switch (difficulty) {
            case EASY -> easyStats;
            case HARD -> hardStats;
            default -> mediumStats;
            };
        }

        /**
         * Indicates whether at least one game completion was recorded.
         *
         * @return {@code true} when any difficulty has one or more wins.
         */
        public boolean hasAnyStats() {
            return easyStats.hasResults() || mediumStats.hasResults() || hardStats.hasResults();
        }
    }

    /**
     * Immutable aggregate for a single difficulty.
     */
    public static final class DifficultyStats {
        private final int wins;
        private final long bestTimeInMillis;
        private final int bestScore;

        DifficultyStats(int wins, long bestTimeInMillis, int bestScore) {
            this.wins = wins;
            this.bestTimeInMillis = bestTimeInMillis;
            this.bestScore = bestScore;
        }

        /**
         * @return Total wins recorded for this difficulty.
         */
        public int getWins() {
            return wins;
        }

        /**
         * @return Fastest completion time in milliseconds, or {@code -1} when unavailable.
         */
        public long getBestTimeInMillis() {
            return bestTimeInMillis;
        }

        /**
         * @return Highest score for this difficulty, or {@code -1} when unavailable.
         */
        public int getBestScore() {
            return bestScore;
        }

        /**
         * @return {@code true} when at least one result exists for this difficulty.
         */
        public boolean hasResults() {
            return wins > 0;
        }
    }
}
