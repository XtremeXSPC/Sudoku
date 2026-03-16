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
 * A custom View that draws the basic Sudoku grid (lines and block backgrounds). The numbers and cell highlighting are
 * handled by an overlay of TextViews placed on top of this view.
 */
public class SudokuGridView extends View {

    private final Paint paintThinLine;
    private final Paint paintThickLine;
    private final Paint paintBlockBackgroundLight;
    private final Paint paintBlockBackgroundDark;

    /**
     * Calculates the side length of one cell based on the measured width.
     *
     * @return The calculated size for a single cell, or {@code 0f} until layout is complete.
     */
    private float getCellSize() {
        return (getWidth() > 0) ? (float) getWidth() / 9.0f : 0f;
    }

    /**
     * Creates the view from code.
     */
    public SudokuGridView(Context context) {
        this(context, null);
    }

    /**
     * Creates the view from XML inflation.
     */
    public SudokuGridView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Main constructor used by Android inflation and style resolution.
     */
    public SudokuGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paintThinLine = new Paint();
        paintThinLine.setStyle(Paint.Style.STROKE);
        paintThinLine.setStrokeWidth(2.0f); // Thinner line
        paintThinLine.setAntiAlias(true);
        paintThinLine.setColor(ContextCompat.getColor(context, R.color.grid_line_thin));

        paintThickLine = new Paint();
        paintThickLine.setStyle(Paint.Style.STROKE);
        paintThickLine.setStrokeWidth(4.5f); // Softer thick line for separation
        paintThickLine.setAntiAlias(true);
        paintThickLine.setColor(ContextCompat.getColor(context, R.color.grid_line_thick));

        paintBlockBackgroundLight = new Paint();
        paintBlockBackgroundLight.setStyle(Paint.Style.FILL);
        paintBlockBackgroundLight.setAntiAlias(true);
        paintBlockBackgroundLight
                .setColor(ContextCompat.getColor(context, R.color.cell_background_light));

        paintBlockBackgroundDark = new Paint();
        paintBlockBackgroundDark.setStyle(Paint.Style.FILL);
        paintBlockBackgroundDark.setAntiAlias(true);
        paintBlockBackgroundDark
                .setColor(ContextCompat.getColor(context, R.color.cell_background_dark));
    }


    /**
     * Ensures the view is always square. It takes the smaller of the width and height.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }

    /**
     * The main drawing method. It first draws the background blocks, then the grid lines.
     *
     * @param canvas The canvas on which the background will be drawn.
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (getCellSize() == 0f) {
            return;
        }

        drawBackgroundBlocks(canvas);
        drawLines(canvas);
    }

    /**
     * Draws the alternating light/dark backgrounds for the 3x3 blocks.
     */
    private void drawBackgroundBlocks(Canvas canvas) {
        float cellSize = getCellSize();
        for (int rowBlock = 0; rowBlock < 3; rowBlock++) {
            for (int colBlock = 0; colBlock < 3; colBlock++) {
                Paint paint;
                if ((rowBlock + colBlock) % 2 == 0) {
                    paint = paintBlockBackgroundLight;
                } else {
                    paint = paintBlockBackgroundDark;
                }

                canvas.drawRect(colBlock * 3 * cellSize, rowBlock * 3 * cellSize,
                        (colBlock + 1) * 3 * cellSize, (rowBlock + 1) * 3 * cellSize, paint);
            }
        }
    }

    /**
     * Draws the horizontal and vertical lines of the Sudoku grid.
     */
    private void drawLines(Canvas canvas) {
        float cellSize = getCellSize();
        float viewSize = (float) getWidth(); // or getHeight(), since it's a square

        // Draw vertical lines
        for (int i = 0; i <= 9; i++) {
            float x = i * cellSize;
            Paint paint = (i % 3 == 0) ? paintThickLine : paintThinLine;
            canvas.drawLine(x, 0f, x, viewSize, paint);
        }

        // Draw horizontal lines
        for (int i = 0; i <= 9; i++) {
            float y = i * cellSize;
            Paint paint = (i % 3 == 0) ? paintThickLine : paintThinLine;
            canvas.drawLine(0f, y, viewSize, y, paint);
        }
    }
}
