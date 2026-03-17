# Architecture

This app mixes a small Compose entry point with a ViewBinding-driven game screen, wrapped in an MVVM stack that keeps state out of the Activities. The puzzle engine is isolated from Android APIs to keep it testable.

## UI flow

- `HomeActivity` renders a simple Compose screen (`DifficultyScreen`) that can either start a fresh puzzle, resume the latest saved game, or open the dedicated statistics page.
- `StatsActivity` renders the local statistics overview and the per-difficulty breakdown backed by `GameStatsStore`.
- `MainActivity` owns the view hierarchy defined in XML and overlays `TextView` cells on top of two custom views:
  - `SudokuGridView` draws the board background and lines.
  - `HighlightOverlayView` draws selection, row/column, and 3x3 block highlights.
- All user input (number pad, undo, new game, cell taps) goes through the `SudokuViewModel`.

## State management

- `SudokuViewModel` exposes immutable `LiveData` for the board, selected cell, elapsed time, errors, score, and generation flags.
- Puzzle generation runs on a single-thread `ExecutorService`; results are posted back to the main thread and update LiveData.
- A `Handler` on the main looper advances the in-app timer every second; timer state is paused or restarted when games finish or resume.
- `saveState()`/`restoreState()` pair the parcelable `SudokuBoard` with a `Bundle` to survive process death and configuration changes.
- `SavedGameStore` serializes that same pair into `SharedPreferences`, letting the app restore the latest in-progress session after a full app closure.
- `GameStatsStore` keeps a tiny local stats surface: wins, best time, and best score per difficulty, which `StatsActivity` aggregates into a lightweight summary.

## Game logic

- `SudokuBoard` owns the solution grid, the user-facing grid of `SudokuCell` objects, and a stack of `MoveRecord` entries to support undo.
- Puzzle generation uses recursive backtracking to build a full solution, then removes numbers while checking for a unique solution (`countUniqueSolutions` short-circuits after finding more than one).
- Validation helpers:
  - `isMoveCorrect` compares user input with the solution grid.
  - `isCurrentBoardStateValidAccordingToRules` performs rule-only validation on the current grid (no duplicates in rows/cols/blocks).
  - `areAllUserCellsCorrect` verifies that all user-entered numbers match the solution when the board is full.
- Scoring and errors:
  - Positive points per correct entry scale with difficulty; incorrect moves apply a softer difficulty-scaled penalty, never dropping the score below zero.
  - Undo re-applies the stored score delta, but the error counter is cumulative across the whole match and does not decrease on undo.
  - Finishing a valid board awards a time-based bonus plus a difficulty bonus.

## Rendering details

- Cells are drawn as `TextView`s inside a `GridLayout` laid over the custom grid and highlight views. Colors reflect fixed cells, correct inputs, wrong inputs, and empty cells.
- Highlighting uses the container width to compute cell size, so it adapts when the device rotates.
- Compose is limited to the home/statistics screens and theme definitions; the game screen remains in the View system for simplicity.

## Threading and performance

- Generation work is cancelable; starting a new game cancels any in-flight generation before submitting a fresh task.
- Timer updates and LiveData delivery happen on the main thread; only puzzle generation and validation copies run off the UI thread.
- In-progress sessions and local stats are stored in `SharedPreferences`; the actual puzzle engine and gameplay state still live in memory while the app is active.
