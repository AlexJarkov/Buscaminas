package buscaminas;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class MinesweeperGameTest {

    @Test
    void firstClickHasNoMineAndZeroAdjacencyWhenPossible() {
        MinesweeperGame g = new MinesweeperGame(9,9,10, new Random(123));
        g.reset();
        MinesweeperGame.OpenResult res = g.openCell(4,4);
        assertFalse(res.exploded);
        assertTrue(g.isOpened(4,4));
        assertEquals(0, g.countAdjacentMines(4,4));
        // ensure no mines in 3x3 halo
        for (int r=3;r<=5;r++) for (int c=3;c<=5;c++) assertFalse(g.isMine(r,c));
    }

    @Test
    void flagPreventsOpenAndCounts() {
        MinesweeperGame g = new MinesweeperGame(9,9,10, new Random(1));
        g.reset();
        g.toggleFlag(0,0);
        assertTrue(g.isFlagged(0,0));
        int before = g.getFlagsCount();
        MinesweeperGame.OpenResult res = g.openCell(0,0);
        assertFalse(res.exploded);
        assertFalse(g.isOpened(0,0));
        assertEquals(before, g.getFlagsCount());
    }

    @Test
    void chordOpensNeighborsOrExplodesIfWrong() {
        MinesweeperGame g = new MinesweeperGame(9,9,10, new Random(42));
        g.reset();
        g.openCell(0,0); // place mines
        // Find a non-zero opened cell near by opening 0,0 area
        boolean found = false;
        outer:
        for (int r=0;r<g.getRows();r++) {
            for (int c=0;c<g.getCols();c++) {
                if (!g.isMine(r,c)) {
                    g.openCell(r,c);
                    if (g.countAdjacentMines(r,c) > 0 && g.isOpened(r,c)) {
                        // place flags equal to count on actual mines to make chord safe
                        int need = g.countAdjacentMines(r,c);
                        for (int dr=-1; dr<=1; dr++)
                            for (int dc=-1; dc<=1; dc++) {
                                if (dr==0 && dc==0) continue;
                                int nr=r+dr,nc=c+dc;
                                if (nr>=0&&nr<g.getRows()&&nc>=0&&nc<g.getCols()&&g.isMine(nr,nc)) {
                                    if (!g.isFlagged(nr,nc)) g.toggleFlag(nr,nc);
                                }
                            }
                        MinesweeperGame.OpenResult chord = g.chordOpen(r,c);
                        assertFalse(chord.exploded);
                        found = true;
                        break outer;
                    }
                }
            }
        }
        assertTrue(found);
    }
}

