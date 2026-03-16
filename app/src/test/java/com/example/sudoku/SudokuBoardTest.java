package com.example.sudoku;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SudokuBoardTest {

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

    @Test
    public void setCellValueAndUndo_restorePreviousValueAndCorrectness() throws Exception {
        SudokuBoard board = createBoardWithOpenCells(SudokuBoard.Difficulty.EASY, new int[][] { { 0, 0 } });

        board.setCellValue(0, 0, SOLUTION[0][0], 10, false);

        SudokuCell updatedCell = board.getCell(0, 0);
        assertEquals(SOLUTION[0][0], updatedCell.getValue());
        assertTrue(updatedCell.isCorrect());

        SudokuBoard.MoveRecord undoneMove = board.undoMove();

        assertNotNull(undoneMove);
        assertEquals(10, undoneMove.getScoreChange());
        assertFalse(undoneMove.wasError());
        assertEquals(0, board.getCell(0, 0).getValue());
        assertTrue(board.getCell(0, 0).isCorrect());
    }

    @Test
    public void isCurrentBoardStateValidAccordingToRules_detectsDuplicates() throws Exception {
        SudokuBoard board = createBoardWithOpenCells(SudokuBoard.Difficulty.MEDIUM, new int[][] {});
        SudokuCell[][] cells = getBoardCells(board);
        cells[0][0] = new SudokuCell(1, false, true, null);
        cells[0][1] = new SudokuCell(1, false, true, null);
        setField(board, "board", cells);

        assertFalse(board.isCurrentBoardStateValidAccordingToRules());
    }

    @Test(timeout = 30000)
    public void generateNewPuzzle_createsExpectedNumberOfFixedAndEditableCells() throws Exception {
        for (SudokuBoard.Difficulty difficulty : SudokuBoard.Difficulty.values()) {
            SudokuBoard board = new SudokuBoard();

            board.generateNewPuzzle(difficulty);

            int emptyCells = 0;
            int fixedCells = 0;
            int filledEditableCells = 0;
            for (int row = 0; row < 9; row++) {
                for (int col = 0; col < 9; col++) {
                    SudokuCell cell = board.getCell(row, col);
                    if (cell.getValue() == 0) {
                        emptyCells++;
                        assertFalse(cell.isFixed());
                    } else if (cell.isFixed()) {
                        fixedCells++;
                    } else {
                        filledEditableCells++;
                    }
                }
            }

            assertEquals(difficulty, board.getCurrentDifficulty());
            assertEquals(difficulty.cellsToRemove, emptyCells);
            assertEquals(81 - difficulty.cellsToRemove, fixedCells);
            assertEquals(0, filledEditableCells);
            assertFalse(board.isBoardFull());
            assertTrue(board.isCurrentBoardStateValidAccordingToRules());
            assertTrue(board.areAllUserCellsCorrect());
        }
    }

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

    private SudokuCell[][] getBoardCells(SudokuBoard board) throws Exception {
        Field boardField = SudokuBoard.class.getDeclaredField("board");
        boardField.setAccessible(true);
        return (SudokuCell[][]) boardField.get(board);
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
