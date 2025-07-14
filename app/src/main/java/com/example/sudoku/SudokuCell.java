package com.example.sudoku;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a single cell within a Sudoku grid.
 * Each cell contains a numeric value, fixed state, correctness flag, and user notes.
 * Implements {@link Parcelable} for Android component communication.
 */
public class SudokuCell implements Parcelable {

    // Fields
    private int value;          // The numeric value of the cell (0 if empty)
    private boolean isFixed;    // true if the value is preset and unmodifiable
    private boolean isCorrect;  // true if the current value is correct
    private Set<Integer> notes; // Set of notes entered by the user

    /**
     * Constructs a new {@code SudokuCell} instance.
     *
     * @param value The initial value of the cell.
     * @param isFixed {@code true} if the cell is fixed, {@code false} otherwise.
     * @param isCorrect {@code true} if the cell's value is correct, {@code false} otherwise.
     * @param notes An initial set of notes for the cell. If {@code null}, a new empty {@link HashSet} is created.
     */
    public SudokuCell(int value, boolean isFixed, boolean isCorrect, Set<Integer> notes) {
        this.value = value;
        this.isFixed = isFixed;
        this.isCorrect = isCorrect;
        this.notes = (notes != null) ? new HashSet<>(notes) : new HashSet<>();
    }

    /**
     * Convenience constructor to create an empty, non-fixed cell, considered correct by default.
     * Notes are initialized as an empty set.
     */
    public SudokuCell() {
        this(0, false, true, new HashSet<>());
    }

    // Getter and Setter Methods

    /**
     * Returns the current value of the cell.
     * @return The value of the cell (0 if empty).
     */
    public int getValue() {
        return value;
    }

    /**
     * Sets the value of the cell.
     * @param value The new value for the cell.
     */
    public void setValue(int value) {
        this.value = value;
    }

    /**
     * Checks if the cell is fixed (not modifiable by the user).
     * @return {@code true} if the cell is fixed, {@code false} otherwise.
     */
    public boolean isFixed() {
        return isFixed;
    }

    /**
     * Sets the "fixed" state of the cell.
     * @param fixed {@code true} to make the cell fixed, {@code false} otherwise.
     */
    public void setFixed(boolean fixed) {
        isFixed = fixed;
    }

    /**
     * Checks if the current value of the cell is considered correct.
     * @return {@code true} if the value is correct, {@code false} otherwise.
     */
    public boolean isCorrect() {
        return isCorrect;
    }

    /**
     * Sets the correctness state of the cell.
     * @param correct {@code true} if the value is correct, {@code false} otherwise.
     */
    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    /**
     * Returns the set of notes associated with this cell.
     * @return A {@link Set} containing the notes.
     */
    public Set<Integer> getNotes() {
        return notes;
    }

    /**
     * Sets the set of notes for this cell.
     * @param notes The new set of notes.
     */
    public void setNotes(Set<Integer> notes) {
        this.notes = (notes != null) ? new HashSet<>(notes) : new HashSet<>();
    }

    // Business Logic

    /**
     * Adds a note to the cell, if the cell is not fixed and the note is a valid number (1-9).
     * @param note The note to add.
     */
    public void addNote(int note) {
        if (!isFixed && note >= 1 && note <= 9) {
            notes.add(note);
        }
    }

    /**
     * Removes a specified note from the cell.
     * @param note The note to remove.
     */
    public void removeNote(int note) {
        notes.remove(note);
    }

    /**
     * Removes all notes from the cell.
     */
    public void clearNotes() {
        notes.clear();
    }

    /**
     * Resets the cell to its initial state for user input:
     * value to 0, correctness state to {@code true}, and clears all notes.
     * This operation is only performed if the cell is not fixed.
     */
    public void resetUserEntry() {
        if (!isFixed) {
            value = 0;
            isCorrect = true; // An empty cell is considered correct until an incorrect value is entered
            clearNotes();
        }
    }

    // Parcelable Implementation

    /**
     * Constructor used to create an instance of {@code SudokuCell} from a {@link Parcel}.
     * This constructor is called internally by the {@link #CREATOR}.
     *
     * @param in The Parcel from which to read the cell's data.
     */
    protected SudokuCell(Parcel in) {
        value = in.readInt();
        isFixed = in.readByte() != 0; // Reads a byte and converts it to boolean
        isCorrect = in.readByte() != 0; // Reads a byte and converts it to boolean

        int[] notesArray = in.createIntArray();
        notes = new HashSet<>();
        if (notesArray != null) {
            for (int note : notesArray) {
                notes.add(note);
            }
        }
    }

    /**
     * Static field required by the {@link Parcelable} interface.
     * Used by the Android system to create new instances of the {@code SudokuCell} class
     * from a {@link Parcel}.
     */
    public static final Creator<SudokuCell> CREATOR = new Creator<>() {
        /**
         * Creates a new instance of {@code SudokuCell}, populating it with data read from the Parcel.
         * @param in The Parcel to read data from.
         * @return A new instance of {@code SudokuCell}.
         */
        @Override
        public SudokuCell createFromParcel(Parcel in) {
            return new SudokuCell(in);
        }

        /**
         * Creates a new array of {@code SudokuCell}.
         * @param size The size of the array to create.
         * @return An array of {@code SudokuCell}.
         */
        @Override
        public SudokuCell[] newArray(int size) {
            return new SudokuCell[size];
        }
    };

    /**
     * Describes the kinds of special objects contained in this Parcelable instance's marshaled representation.
     * @return A bitmask indicating the set of special object types marshaled.
     */
    @Override
    public int describeContents() {
        return 0; // Usually 0 unless you are dealing with FileDescriptors
    }

    /**
     * Writes the current state of the object to a {@link Parcel}.
     * @param dest The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *              May be 0 or {@link Parcelable#PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(value);
        dest.writeByte((byte) (isFixed ? 1 : 0)); // Writes boolean as byte
        dest.writeByte((byte) (isCorrect ? 1 : 0)); // Writes boolean as byte

        // Converts the Set of notes to an int array for writing to the Parcel
        int[] notesArray = new int[notes.size()];
        int i = 0;
        for (Integer note : notes) {
            notesArray[i++] = note;
        }
        dest.writeIntArray(notesArray);
    }
}
