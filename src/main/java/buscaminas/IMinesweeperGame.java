package buscaminas;

/**
 * Interface for the Minesweeper game model used by the UI.
 * The concrete implementation remains {@link MinesweeperGame}.
 */
public interface IMinesweeperGame {
    int getRows();
    int getCols();
    int getTotalMines();

    boolean isMine(int r, int c);
    boolean isOpened(int r, int c);
    boolean isFlagged(int r, int c);

    boolean[][] getMines();
    boolean[][] getOpened();

    int countAdjacentMines(int r, int c);
    int countAdjacentFlags(int r, int c);

    int getFlagsCount();
    void toggleFlag(int r, int c);

    IOpenResult openCell(int r, int c);
    IOpenResult chordOpen(int r, int c);

    boolean isWin();

    void reset();
}

