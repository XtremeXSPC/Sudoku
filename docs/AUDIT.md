# Android Sudoku — Code Audit

**Date:** 2026-03-16
**Auditor:** Claude Sonnet 4.6
**Scope:** Full source audit — architecture, game logic, UI, code quality, performance

---

## 1. Architecture & Structure

The app follows clean MVVM with well-separated layers: `SudokuBoard` (model), `SudokuViewModel`
(state/logic), `MainActivity` (UI observer), and two store classes for persistence. There are no
god classes. The hybrid Compose/ViewBinding split (HomeActivity + StatsActivity use Compose,
MainActivity uses XML ViewBinding) is internally consistent but creates a long-term migration
obligation.

| Finding                                                                                                                                                                                                                                                    | Severity |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| `SudokuBoard` manages both domain state and undo history (`Stack<MoveRecord>`), coupling game logic with the undo mechanism. A dedicated `GameHistory` class would improve separation. (`SudokuBoard.java:73`)                                             | [MEDIUM] |
| `SudokuViewModel` declares two `Handler` objects (`mainHandler`, `timerHandler`) both bound to `Looper.getMainLooper()`. They are functionally identical; one is redundant. (`SudokuViewModel.java:60–61`)                                                 | [LOW]    |
| `MoveRecord` public static inner class has package-private fields (`row`, `col`, `oldValue`, `newValue`) rather than `private`. It already has getters for the two fields external code uses; the raw fields should be private. (`SudokuBoard.java:39–44`) | [LOW]    |
| Hybrid UI architecture (Compose for shell screens, ViewBinding for gameplay) is valid but creates diverging patterns. As the project grows, new screens will need a consistent choice.                                                                     | [LOW]    |

---

## 2. Game Logic

Backtracking generation and the uniqueness solver (`countUniqueSolutions`) are correct. The
solver short-circuits at 2 solutions, which is the standard and efficient approach. Difficulty
constants are semantically accurate (EASY removes 35 cells → 46 clues; HARD removes 55 → 26
clues). Interruption support is properly wired through both generation loops.

| Finding                                                                                                                                                                                                                                                                                                                                                                | Severity |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| `generateNewPuzzle()` throws `IllegalStateException` after 8 failed attempts. The ViewModel's `catch (Exception e)` block silently swallows this — it only hides the loading spinner. Users see a blank grid with no error message. (`SudokuBoard.java:133`, `SudokuViewModel.java:158–165`)                                                                           | [MEDIUM] |
| `areAllUserCellsCorrect()` and `isCurrentBoardStateValidAccordingToRules()` are both called in `checkGameStatus()` on every cell input, resulting in two full 81-cell scans on every move. The rules check is redundant when individual cells are already validated against the solution on entry. (`SudokuViewModel.java:451`, `SudokuBoard.java:242–255`, `279–305`) | [LOW]    |
| `fillBoardRecursive()` allocates a new `ArrayList<Integer>` and calls `Collections.shuffle()` on every recursive invocation — up to 81 allocations per board generation. A reused array with an in-place Fisher-Yates shuffle would eliminate this. (`SudokuBoard.java:346–350`)                                                                                       | [LOW]    |
| `isCurrentBoardStateValidAccordingToRules()` allocates a new `int[9][9]` array on every call, which runs after every cell input. The allocation is small but unnecessary; the method could operate directly on `board` values. (`SudokuBoard.java:281`)                                                                                                                | [LOW]    |

---

## 3. UI Layer

State is correctly driven from ViewModel LiveData observers. The layered rendering
(SudokuGridView → HighlightOverlayView → 81 TextViews) is clean. The main concerns are
redundant draw calls, dynamic cell text sized in raw pixels (breaking font-scale accessibility),
and logic-heavy button construction in Java instead of XML.

| Finding                                                                                                                                                                                                                                                                                                            | Severity |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------- |
| Cell text size is set as `cellSize * 0.65f` using raw pixels, ignoring the system font scale (`TypedValue.COMPLEX_UNIT_PX` instead of `COMPLEX_UNIT_SP`). This breaks accessibility for users who rely on large-text settings. (`MainActivity.java:221`)                                                           | [MEDIUM] |
| `updateGridUI()` calls `updateHighlightOverlay()` at line 337 internally. The `_selectedCell` LiveData observer in `observeViewModel()` also calls `updateHighlightOverlay()`. On every board change this triggers two back-to-back `invalidate()` calls on `HighlightOverlayView`. (`MainActivity.java:249, 337`) | [MEDIUM] |
| `initializeSudokuGridOverlay()` adds 81 `TextView` children programmatically. It calls `removeAllViews()` first, but it is registered in a `OnGlobalLayoutListener` which could fire more than once (e.g. on layout re-measurement). A guard is needed. (`MainActivity.java:209–210`)                              | [MEDIUM] |
| `HighlightOverlayView` declares two `Paint` objects for different highlight purposes (`highlightPaintRowCol`, `highlightPaintBlock`) but initializes both to the same color. `highlightPaintBlock` is a redundant duplicate of `highlightPaintRowCol`. (`HighlightOverlayView.java:44–53`)                         | [LOW]    |
| Number pad buttons are constructed entirely in Java code with hardcoded `22f` sp text size, `dpToPx(8)` padding, `dpToPx(2)` elevation, and `dpToPx(4)` margins. These belong in an XML item layout or style resource. (`MainActivity.java:156–184`)                                                               | [LOW]    |
| `tools:text` attributes in `activity_main.xml` use Italian text ("Errori: 0", "Difficile"). These are preview-only and have no runtime effect, but they should match the app's default locale for design tool consistency. (`activity_main.xml:56, 89`)                                                            | [LOW]    |
| `SudokuGridView` resolves colors with `ContextCompat.getColor()` in the constructor. Colors are cached in `Paint` objects and won't reflect a runtime theme change. This is not a current bug but is a latent risk if dynamic theming is added. (`SudokuGridView.java:54–72`)                                      | [LOW]    |

---

## 4. Code Quality

Code is consistently documented and structured. Dead code is minimal. The main quality concern is
`removeCompletionBonusFromScoreIfApplied()`, which performs multiple hidden state mutations as a
side effect, making call-site reasoning difficult.

| Finding                                                                                                                                                                                                                                                                                                                                                                 | Severity |
| ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| `removeCompletionBonusFromScoreIfApplied()` mutates five separate pieces of state (`completionBonusApplied`, `awardedCompletionBonus`, `_score`, `_isGameWon`, `_isGameOverWithIncorrectBoard`) as side effects of what appears to be a read-and-remove operation. This makes it hard to reason about when called from multiple paths. (`SudokuViewModel.java:489–502`) | [MEDIUM] |
| `SudokuCell.getNotes()` returns the internal `HashSet<Integer>` directly. Callers can mutate the notes set silently, bypassing `addNote()` validation (which enforces 1–9 range and non-fixed guard). (`SudokuCell.java:107–109`)                                                                                                                                       | [MEDIUM] |
| `SavedGameStore.clear()` calls `editor.clear()`, which wipes the entire SharedPreferences file rather than just the two game keys. If other keys are ever added to this preferences file, they will be unintentionally erased. (`SavedGameStore.java:58`)                                                                                                               | [LOW]    |
| Commented-out dead code in `SudokuViewModel` constructor: `// startNewGame(SudokuBoard.Difficulty.MEDIUM);`. (`SudokuViewModel.java:112`)                                                                                                                                                                                                                               | [LOW]    |
| `@SuppressLint("SetTextI18n")` is applied at method level on `updateGridUI()`. The suppression is justified (numeric strings are locale-neutral), but it silences lint for the entire method. A comment explaining the rationale would help future reviewers. (`MainActivity.java:298`)                                                                                 | [LOW]    |

---

## 5. Performance

Puzzle generation is correctly off the main thread with proper cancellation. The 1-second Handler
timer is efficient. The main performance concern is up to 81 simultaneous `ObjectAnimator`
instances on full board updates.

| Finding                                                                                                                                                                                                                                                                                                                                                            | Severity |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------- |
| `updateGridUI()` can start up to 81 concurrent `ObjectAnimator` instances in a single pass (one per cell whose color changes). On operations that update all cells simultaneously (undo to start state, initial board load, restore from saved game), 81 parallel animators run at once, which can cause jank on mid-range hardware. (`MainActivity.java:326–329`) | [MEDIUM] |
| `updateHighlightOverlay()` is called redundantly on every board update (from inside `updateGridUI()` and again from the `_selectedCell` observer). While `invalidate()` is coalesced by the View system, the double-call is wasteful. (`MainActivity.java:249, 337`)                                                                                               | [LOW]    |
| `SudokuGridView.getCellSize()` recomputes `getWidth() / 9.0f` on each call. It is called twice per `onDraw()` — once from `drawBackgroundBlocks()` and once from `drawLines()`. The value should be computed once in `onDraw()` and passed down. (`SudokuGridView.java:31–33, 105–106, 125–127`)                                                                   | [LOW]    |

---

## Top 5 Prioritized Improvements

1. **[MEDIUM] Expose puzzle generation failure to the user.**
   `startNewGame()` catches `Exception` silently after `generateNewPuzzle()` exhausts its 8 retry
   attempts and throws `IllegalStateException`. Add an `_generationError` `MutableLiveData<Boolean>`
   (or reuse a generic error state), post `true` in the catch block, and show a Toast/dialog in
   the Activity observer. Files: `SudokuViewModel.java:158–165`, `SudokuBoard.java:133`.

2. **[MEDIUM] Fix cell text size to respect system font scale.**
   Replace `TypedValue.COMPLEX_UNIT_PX` with `TypedValue.COMPLEX_UNIT_SP` in
   `initializeSudokuGridOverlay()`, or compute the SP equivalent via `TypedValue.applyDimension`.
   This ensures the grid is accessible to users with large-text accessibility settings.
   File: `MainActivity.java:221`.

3. **[MEDIUM] Fix mutable internal state exposure in `SudokuCell.getNotes()`.**
   Change the return to `Collections.unmodifiableSet(notes)` so external callers cannot silently
   add arbitrary values (e.g. 0 or > 9) that bypass `addNote()` validation.
   File: `SudokuCell.java:107–109`.

4. **[MEDIUM] Remove the redundant `updateHighlightOverlay()` call from `updateGridUI()`.**
   `updateGridUI()` calls `updateHighlightOverlay()` on line 337, but the `_selectedCell`
   LiveData observer already handles this. Delete line 337 and rely solely on the observer.
   This also eliminates the performance finding. File: `MainActivity.java:337`.

5. **[MEDIUM] Guard `ObjectAnimator` spawning in `updateGridUI()`.**
   Add a check so animations are only started for cells whose color actually changes (which is
   already attempted at line 325, but the `oldColor != onBackground` guard is too broad — it
   skips animation only if the old color happens to equal `onBackground`, not if it equals
   `newColor`). The condition on line 325 should be:
   `if (oldColor != newColor)` (remove the second clause). This prevents unnecessary animators
   and ensures correct behavior for the fixed-color case. File: `MainActivity.java:325`.
