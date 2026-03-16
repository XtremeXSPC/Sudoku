package com.example.sudoku;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SudokuCellTest {

    @Test
    public void getNotes_returnsUnmodifiableView() {
        SudokuCell cell = new SudokuCell();
        cell.addNote(3);

        Set<Integer> notes = cell.getNotes();

        assertEquals(1, notes.size());
        try {
            notes.add(9);
            fail("Expected notes view to be immutable");
        } catch (UnsupportedOperationException expected) {
            assertEquals(1, cell.getNotes().size());
        }
    }
}
