package buscaminas;

import java.util.*;

public class MinesweeperGame {
    private final int rows;
    private final int cols;
    private final int totalMines;

    private final boolean[][] mines;   // Posición de las minas
    private final boolean[][] opened;  // Cajas abiertas

    private boolean minesPlaced = false;
    private int openedSafeCells = 0;
    private final Random random = new Random();

    public static class OpenResult {
        public final boolean exploded;
        public final List<int[]> openedCells; // cada elemento es {row, col}

        public OpenResult(boolean exploded, List<int[]> openedCells) {
            this.exploded = exploded;
            this.openedCells = openedCells;
        }
    }

    public MinesweeperGame(int rows, int cols, int totalMines) {
        if (rows <= 0 || cols <= 0) throw new IllegalArgumentException("Dimensiones inválidas");
        if (totalMines < 0 || totalMines >= rows * cols)
            throw new IllegalArgumentException("Número de minas inválido");
        this.rows = rows;
        this.cols = cols;
        this.totalMines = totalMines;
        this.mines = new boolean[rows][cols];
        this.opened = new boolean[rows][cols];
    }

    public void reset() {
        for (int r = 0; r < rows; r++) {
            Arrays.fill(mines[r], false);
            Arrays.fill(opened[r], false);
        }
        minesPlaced = false;
        openedSafeCells = 0;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    private void placeMinesAvoiding(int safeR, int safeC) {
        int placed = 0;
        while (placed < totalMines) {
            int r = random.nextInt(rows);
            int c = random.nextInt(cols);
            if ((r == safeR && c == safeC) || mines[r][c]) continue;
            mines[r][c] = true;
            placed++;
        }
        minesPlaced = true;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getTotalMines() { return totalMines; }
    public boolean[][] getMines() { return mines; }
    public boolean[][] getOpened() { return opened; }
    public boolean isMine(int r, int c) { return mines[r][c]; }
    public boolean isOpened(int r, int c) { return opened[r][c]; }

    public int countAdjacentMines(int r, int c) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (inBounds(nr, nc) && mines[nr][nc]) count++;
            }
        }
        return count;
    }

    public OpenResult openCell(int r, int c) {
        if (!inBounds(r, c)) return new OpenResult(false, Collections.emptyList());

        if (!minesPlaced) {
            placeMinesAvoiding(r, c); // primera jugada segura
        }

        if (mines[r][c]) {
            // Abrir la mina clicada
            if (!opened[r][c]) {
                opened[r][c] = true; // marcar como abierta para que la UI la muestre
            }
            return new OpenResult(true, Collections.singletonList(new int[]{r, c}));
        }

        List<int[]> result = new ArrayList<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        boolean[][] visited = new boolean[rows][cols];

        q.add(new int[]{r, c});
        visited[r][c] = true;

        while (!q.isEmpty()) {
            int[] cur = q.removeFirst();
            int cr = cur[0], cc = cur[1];
            if (opened[cr][cc]) continue;
            opened[cr][cc] = true;
            openedSafeCells++;
            result.add(new int[]{cr, cc});

            int adj = countAdjacentMines(cr, cc);
            if (adj == 0) {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = cr + dr, nc = cc + dc;
                        if (inBounds(nr, nc) && !visited[nr][nc] && !mines[nr][nc] && !opened[nr][nc]) {
                            visited[nr][nc] = true;
                            q.addLast(new int[]{nr, nc});
                        }
                    }
                }
            }
        }

        return new OpenResult(false, result);
    }

    public boolean isWin() {
        return openedSafeCells >= (rows * cols - totalMines);
    }
}

