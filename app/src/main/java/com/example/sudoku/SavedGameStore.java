package com.example.sudoku;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Persists the latest in-progress game so it can be resumed after the app is fully closed.
 */
public final class SavedGameStore {

    private static final String PREFS_NAME = "saved_game_preferences";
    private static final String KEY_BOARD = "board";
    private static final String KEY_VIEW_MODEL_STATE = "viewModelState";

    private SavedGameStore() {
    }

    /**
     * Persists both the logical board and the ViewModel bundle as Base64-encoded parcel payloads.
     *
     * @param context Android context used to access {@link SharedPreferences}.
     * @param board Current board snapshot.
     * @param viewModelState Serializable ViewModel state bundle.
     */
    public static void save(@NonNull Context context, @NonNull SudokuBoard board, @NonNull Bundle viewModelState) {
        SharedPreferences preferences = getPreferences(context);
        preferences.edit()
                .putString(KEY_BOARD, marshallParcelable(board))
                .putString(KEY_VIEW_MODEL_STATE, marshallBundle(viewModelState))
                .apply();
    }

    /**
     * Checks whether both required payloads are available in storage.
     *
     * @param context Android context used to access {@link SharedPreferences}.
     * @return {@code true} when a complete saved game is present.
     */
    public static boolean hasSavedGame(@NonNull Context context) {
        SharedPreferences preferences = getPreferences(context);
        return preferences.contains(KEY_BOARD) && preferences.contains(KEY_VIEW_MODEL_STATE);
    }

    /**
     * Loads a previously persisted game snapshot.
     * Corrupted payloads are treated as stale data and cleared to avoid repeated crashes.
     *
     * @param context Android context used to access {@link SharedPreferences}.
     * @return The saved game, or {@code null} if absent or invalid.
     */
    @Nullable
    public static SavedGame load(@NonNull Context context) {
        SharedPreferences preferences = getPreferences(context);
        String encodedBoard = preferences.getString(KEY_BOARD, null);
        String encodedBundle = preferences.getString(KEY_VIEW_MODEL_STATE, null);
        if (encodedBoard == null || encodedBundle == null) {
            return null;
        }

        try {
            SudokuBoard board = unmarshallParcelable(encodedBoard, SudokuBoard.CREATOR);
            Bundle viewModelState = unmarshallBundle(encodedBundle);
            return new SavedGame(board, viewModelState);
        } catch (RuntimeException exception) {
            // Any decoding/parcel failure means payloads are out of sync or stale.
            clear(context);
            return null;
        }
    }

    /**
     * Removes only the keys managed by this store, preserving unrelated preferences.
     *
     * @param context Android context used to access {@link SharedPreferences}.
     */
    public static void clear(@NonNull Context context) {
        getPreferences(context).edit()
                .remove(KEY_BOARD)
                .remove(KEY_VIEW_MODEL_STATE)
                .apply();
    }

    @NonNull
    private static SharedPreferences getPreferences(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    private static String marshallBundle(@NonNull Bundle bundle) {
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeBundle(bundle);
            return Base64.encodeToString(parcel.marshall(), Base64.NO_WRAP);
        } finally {
            parcel.recycle();
        }
    }

    @NonNull
    private static Bundle unmarshallBundle(@NonNull String encodedBundle) {
        Parcel parcel = Parcel.obtain();
        try {
            byte[] bytes = Base64.decode(encodedBundle, Base64.DEFAULT);
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            Bundle bundle = parcel.readBundle(SavedGameStore.class.getClassLoader());
            return bundle != null ? bundle : new Bundle();
        } finally {
            parcel.recycle();
        }
    }

    @NonNull
    private static <T extends Parcelable> String marshallParcelable(@NonNull T value) {
        Parcel parcel = Parcel.obtain();
        try {
            value.writeToParcel(parcel, 0);
            return Base64.encodeToString(parcel.marshall(), Base64.NO_WRAP);
        } finally {
            parcel.recycle();
        }
    }

    @NonNull
    private static <T extends Parcelable> T unmarshallParcelable(@NonNull String encodedValue,
            @NonNull Parcelable.Creator<T> creator) {
        Parcel parcel = Parcel.obtain();
        try {
            byte[] bytes = Base64.decode(encodedValue, Base64.DEFAULT);
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            return creator.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }

    /**
     * Immutable payload representing a persisted game.
     */
    public static final class SavedGame {
        private final SudokuBoard board;
        private final Bundle viewModelState;

        SavedGame(@NonNull SudokuBoard board, @NonNull Bundle viewModelState) {
            this.board = board;
            this.viewModelState = viewModelState;
        }

        /**
         * @return Persisted board snapshot.
         */
        @NonNull
        public SudokuBoard getBoard() {
            return board;
        }

        /**
         * @return Persisted ViewModel bundle.
         */
        @NonNull
        public Bundle getViewModelState() {
            return viewModelState;
        }
    }
}
