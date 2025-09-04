package buscaminas;

import java.util.*;

public class MinesweeperGame {
    public record Cell(int r, int c) {}

    private final int rows;
    private final int cols;
    private final int totalMines;

    private final BitSet mines;   // posición de minas
    private final BitSet opened;  // celdas abiertas
    private final BitSet flagged; // celdas marcadas con bandera

    private boolean minesPlaced = false;
    private int openedSafeCells = 0;
    private final Random random;

    public static class OpenResult {
        public final boolean exploded;
        public final List<Cell> openedCells;

        public OpenResult(boolean exploded, List<Cell> openedCells) {
            this.exploded = exploded;
            this.openedCells = openedCells;
        }
    }

    public MinesweeperGame(int rows, int cols, int totalMines) { this(rows, cols, totalMines, new Random()); }

    public MinesweeperGame(int rows, int cols, int totalMines, Random random) {
        if (rows <= 0 || cols <= 0) throw new IllegalArgumentException("Dimensiones inválidas");
        if (totalMines < 0 || totalMines >= rows * cols) throw new IllegalArgumentException("Número de minas inválido");
        this.rows = rows;
        this.cols = cols;
        this.totalMines = totalMines;
        this.random = Objects.requireNonNull(random);
        this.mines = new BitSet(rows * cols);
        this.opened = new BitSet(rows * cols);
        this.flagged = new BitSet(rows * cols);
    }

    public void reset() {
        mines.clear();
        opened.clear();
        flagged.clear();
        minesPlaced = false;
        openedSafeCells = 0;
    }

    private int idx(int r, int c) { return r * cols + c; }
    private boolean inBounds(int r, int c) { return r >= 0 && r < rows && c >= 0 && c < cols; }
    private boolean inSafeHalo(int r, int c, int sr, int sc) { return Math.abs(r - sr) <= 1 && Math.abs(c - sc) <= 1; }

    private void placeMinesAvoiding(int safeR, int safeC) {
        int placed = 0;
        int freeCellsExcludingHalo = rows * cols - 9;
        boolean useHalo = totalMines <= freeCellsExcludingHalo;
        while (placed < totalMines) {
            int r = random.nextInt(rows);
            int c = random.nextInt(cols);
            int i = idx(r, c);
            if (mines.get(i)) continue;
            if (useHalo && inSafeHalo(r, c, safeR, safeC)) continue;
            if (!useHalo && (r == safeR && c == safeC)) continue;
            mines.set(i);
            placed++;
        }
        minesPlaced = true;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getTotalMines() { return totalMines; }
    public boolean isMine(int r, int c) { return mines.get(idx(r, c)); }
    public boolean isOpened(int r, int c) { return opened.get(idx(r, c)); }
    public boolean isFlagged(int r, int c) { return flagged.get(idx(r, c)); }

    public boolean[][] getMines() {
        boolean[][] copy = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) copy[r][c] = mines.get(idx(r, c));
        }
        return copy;
    }
    public boolean[][] getOpened() {
        boolean[][] copy = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) copy[r][c] = opened.get(idx(r, c));
        }
        return copy;
    }

    public int countAdjacentMines(int r, int c) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (inBounds(nr, nc) && isMine(nr, nc)) count++;
            }
        }
        return count;
    }

    public int countAdjacentFlags(int r, int c) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (inBounds(nr, nc) && isFlagged(nr, nc)) count++;
            }
        }
        return count;
    }

    public int getFlagsCount() { return flagged.cardinality(); }

    public void toggleFlag(int r, int c) {
        if (!inBounds(r, c)) return;
        int i = idx(r, c);
        if (opened.get(i)) return;
        if (flagged.get(i)) flagged.clear(i); else flagged.set(i);
    }

    public OpenResult openCell(int r, int c) {
        if (!inBounds(r, c)) return new OpenResult(false, Collections.emptyList());
        if (isFlagged(r, c)) return new OpenResult(false, Collections.emptyList());

        if (!minesPlaced) placeMinesAvoiding(r, c);

        if (isMine(r, c)) {
            int i = idx(r, c);
            if (!opened.get(i)) opened.set(i);
            return new OpenResult(true, List.of(new Cell(r, c)));
        }

        List<Cell> result = new ArrayList<>();
        ArrayDeque<Cell> q = new ArrayDeque<>();
        BitSet visited = new BitSet(rows * cols);

        q.add(new Cell(r, c));
        visited.set(idx(r, c));

        while (!q.isEmpty()) {
            Cell cur = q.removeFirst();
            int cr = cur.r, cc = cur.c;
            int ci = idx(cr, cc);
            if (opened.get(ci)) continue;
            opened.set(ci);
            openedSafeCells++;
            result.add(cur);

            int adj = countAdjacentMines(cr, cc);
            if (adj == 0) {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = cr + dr, nc = cc + dc;
                        if (!inBounds(nr, nc)) continue;
                        int ni = idx(nr, nc);
                        if (!visited.get(ni) && !isMine(nr, nc) && !opened.get(ni) && !flagged.get(ni)) {
                            visited.set(ni);
                            q.addLast(new Cell(nr, nc));
                        }
                    }
                }
            }
        }

        return new OpenResult(false, result);
    }

    public OpenResult chordOpen(int r, int c) {
        if (!inBounds(r, c) || !isOpened(r, c)) return new OpenResult(false, Collections.emptyList());
        int adjMines = countAdjacentMines(r, c);
        int adjFlags = countAdjacentFlags(r, c);
        if (adjMines == 0 || adjFlags < adjMines) return new OpenResult(false, Collections.emptyList());

        List<Cell> openedNow = new ArrayList<>();
        boolean exploded = false;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (!inBounds(nr, nc) || isFlagged(nr, nc) || isOpened(nr, nc)) continue;
                if (isMine(nr, nc)) {
                    int ni = idx(nr, nc);
                    opened.set(ni);
                    exploded = true;
                    openedNow.add(new Cell(nr, nc));
                } else {
                    OpenResult res = openCell(nr, nc);
                    openedNow.addAll(res.openedCells);
                    exploded = exploded || res.exploded;
                }
            }
        }
        return new OpenResult(exploded, openedNow);
    }

    public boolean isWin() { return openedSafeCells >= (rows * cols - totalMines); }
}
