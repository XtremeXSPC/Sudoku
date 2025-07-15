package com.example.sudoku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * A custom view that overlays highlights on the Sudoku grid. It highlights the selected cell, the corresponding row and
 * column, and the 3x3 block.
 */
public class HighlightOverlayView extends View {

    /** Paint for highlighting the row and column of the selected cell. */
    private final Paint highlightPaintRowCol;
    /** Paint for highlighting the 3x3 block of the selected cell. */
    private final Paint highlightPaintBlock;
    /** Paint for highlighting the currently selected cell. */
    private final Paint highlightPaintSelectedCell;

    /** The row index of the selected cell. Null if no cell is selected. */
    private Integer selectedRow = null;
    /** The column index of the selected cell. Null if no cell is selected. */
    private Integer selectedCol = null;

    /** The size of a single cell in pixels. */
    private float cellSize = 0f;

    /**
     * Constructor for HighlightOverlayView. Initializes the paint objects used for highlighting.
     * 
     * @param context The context in which the view is created.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public HighlightOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // Initialize paint for row and column highlighting
        highlightPaintRowCol = new Paint();
        highlightPaintRowCol.setColor(ContextCompat.getColor(context, R.color.highlight_row_col));
        highlightPaintRowCol.setStyle(Paint.Style.FILL);

        // Initialize paint for block highlighting
        highlightPaintBlock = new Paint();
        highlightPaintBlock.setColor(ContextCompat.getColor(context, R.color.highlight_block));
        highlightPaintBlock.setStyle(Paint.Style.FILL);

        // Initialize paint for the selected cell
        highlightPaintSelectedCell = new Paint();
        highlightPaintSelectedCell
                .setColor(ContextCompat.getColor(context, R.color.highlight_selected_cell));
        highlightPaintSelectedCell.setStyle(Paint.Style.FILL);
    }

    /**
     * Draws the highlights on the canvas. This method is called when the view needs to be redrawn. It first highlights the
     * 3x3 block, then the row and column, and finally the selected cell itself, ensuring the selected cell is drawn on top.
     * 
     * @param canvas The canvas on which to draw the highlights.
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Do not draw anything if no cell is selected or cell size is not set
        if (selectedRow == null || selectedCol == null || cellSize == 0f) {
            return;
        }

        // Highlight the 3x3 block
        int startRow = (selectedRow / 3) * 3;
        int startCol = (selectedCol / 3) * 3;
        canvas.drawRect(startCol * cellSize, startRow * cellSize, (startCol + 3) * cellSize,
                (startRow + 3) * cellSize, highlightPaintBlock);

        // Highlight the row and column
        canvas.drawRect(0, selectedRow * cellSize, 9 * cellSize, (selectedRow + 1) * cellSize,
                highlightPaintRowCol);
        canvas.drawRect(selectedCol * cellSize, 0, (selectedCol + 1) * cellSize, 9 * cellSize,
                highlightPaintRowCol);

        // Highlight the selected cell on top of everything else
        canvas.drawRect(selectedCol * cellSize, selectedRow * cellSize,
                (selectedCol + 1) * cellSize, (selectedRow + 1) * cellSize,
                highlightPaintSelectedCell);
    }

    /**
     * Sets the cell to be highlighted and triggers a redraw of the view.
     * 
     * @param row The row index of the cell to highlight. Can be null to clear selection.
     * @param col The column index of the cell to highlight. Can be null to clear selection.
     * @param cellSize The size of a single cell in the grid.
     */
    public void highlightCell(Integer row, Integer col, float cellSize) {
        this.selectedRow = row;
        this.selectedCol = col;
        this.cellSize = cellSize;
        invalidate(); // Force the view to redraw
    }
}
