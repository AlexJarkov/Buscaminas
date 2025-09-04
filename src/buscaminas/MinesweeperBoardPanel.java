package buscaminas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

class MinesweeperBoardPanel extends JComponent {
    interface Listener {
        void onCellsOpened(List<MinesweeperGame.Cell> opened, boolean exploded, int clickedR, int clickedC);
        void onFlagToggled();
    }

    private static final Color COLOR_CELL_BG = new Color(222, 222, 222);
    private static final Color COLOR_CELL_OPEN = new Color(240, 240, 240);
    private static final Color COLOR_EXPLODED = new Color(255, 170, 170);
    private static final Color COLOR_MINE_REVEAL = new Color(250, 210, 210);

    private static final Color[] NUM_COLORS = new Color[]{
            Color.DARK_GRAY,
            new Color(0, 102, 204),
            new Color(0, 153, 0),
            new Color(204, 0, 0),
            new Color(0, 0, 153),
            new Color(153, 0, 0),
            new Color(0, 153, 153),
            Color.BLACK,
            Color.GRAY
    };

    private final MinesweeperGame game;
    private final Listener listener;
    private int cellSize = 28;
    private boolean revealAll = false;
    private int explodedR = -1, explodedC = -1;
    private boolean locked = false;
    private Font numberFont;

    MinesweeperBoardPanel(MinesweeperGame game, Listener listener) {
        this.game = game;
        this.listener = listener;
        setOpaque(true);
        setBackground(new Color(200, 200, 200));
        Font base = getFont();
        if (base == null) base = UIManager.getFont("Label.font");
        if (base == null) base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        numberFont = base.deriveFont(Font.BOLD, 14f);

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (locked) return;
                requestFocusInWindow();
                int col = e.getX() / cellSize;
                int row = e.getY() / cellSize;
                if (row < 0 || col < 0 || row >= game.getRows() || col >= game.getCols()) return;

                if (SwingUtilities.isRightMouseButton(e)) {
                    if (!game.isOpened(row, col)) {
                        game.toggleFlag(row, col);
                        if (listener != null) listener.onFlagToggled();
                        repaintCell(row, col);
                    }
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    if (game.isFlagged(row, col)) return;
                    MinesweeperGame.OpenResult res;
                    if (game.isOpened(row, col)) {
                        res = game.chordOpen(row, col);
                    } else {
                        res = game.openCell(row, col);
                    }
                    if (res.exploded) {
                        revealAllMines(row, col);
                    }
                    if (listener != null) listener.onCellsOpened(res.openedCells, res.exploded, row, col);
                    repaint();
                }
            }
        });
    }

    void setLocked(boolean locked) { this.locked = locked; }

    void setCellSize(int cellSize) {
        this.cellSize = Math.max(16, Math.min(48, cellSize));
        Font base = getFont();
        if (base == null) base = UIManager.getFont("Label.font");
        if (base == null) base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        numberFont = base.deriveFont(Font.BOLD, Math.max(10f, this.cellSize * 0.5f));
        revalidate();
        repaint();
    }

    void revealAllMines(int clickedR, int clickedC) {
        this.revealAll = true;
        this.explodedR = clickedR;
        this.explodedC = clickedC;
        this.locked = true;
        repaint();
    }

    @Override public Dimension getPreferredSize() {
        return new Dimension(game.getCols() * cellSize, game.getRows() * cellSize);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int rows = game.getRows();
        int cols = game.getCols();
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = c * cellSize;
                int y = r * cellSize;
                paintCell(g2, r, c, x, y);
            }
        }

        g2.setColor(new Color(180, 180, 180));
        for (int r = 0; r <= rows; r++) g2.drawLine(0, r * cellSize, cols * cellSize, r * cellSize);
        for (int c = 0; c <= cols; c++) g2.drawLine(c * cellSize, 0, c * cellSize, rows * cellSize);
        g2.dispose();
    }

    private void paintCell(Graphics2D g2, int r, int c, int x, int y) {
        if (revealAll && game.isMine(r, c)) {
            g2.setColor((r == explodedR && c == explodedC) ? COLOR_EXPLODED : COLOR_MINE_REVEAL);
            g2.fillRect(x, y, cellSize, cellSize);
            paintBomb(g2, x, y, cellSize);
            return;
        }

        if (game.isOpened(r, c)) {
            g2.setColor(COLOR_CELL_OPEN);
            g2.fillRect(x, y, cellSize, cellSize);
            int adj = game.countAdjacentMines(r, c);
            if (adj > 0) {
                g2.setFont(numberFont);
                g2.setColor(colorForNumber(adj));
                String s = Integer.toString(adj);
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (cellSize - fm.stringWidth(s)) / 2;
                int ty = y + (cellSize + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(s, tx, ty);
            }
        } else {
            g2.setColor(COLOR_CELL_BG);
            g2.fillRect(x, y, cellSize, cellSize);
            if (game.isFlagged(r, c)) paintFlag(g2, x, y, cellSize);
        }
    }

    private void repaintCell(int r, int c) {
        repaint(c * cellSize, r * cellSize, cellSize, cellSize);
    }

    private static Color colorForNumber(int n) { return NUM_COLORS[Math.max(0, Math.min(8, n))]; }

    private static void paintFlag(Graphics2D g2, int x, int y, int size) {
        int poleX = x + size/5;
        g2.setColor(new Color(80,80,80));
        g2.fillRect(poleX, y + size/6, Math.max(2, size/12), size - size/6 - 2);
        int flagW = (int)(size * 0.6);
        int flagH = (int)(size * 0.4);
        int fx = poleX + Math.max(2, size/12);
        int fy = y + size/6;
        Polygon p = new Polygon();
        p.addPoint(fx, fy);
        p.addPoint(fx + flagW, fy + flagH/2);
        p.addPoint(fx, fy + flagH);
        g2.setColor(new Color(200, 30, 30));
        g2.fillPolygon(p);
    }

    private static void paintBomb(Graphics2D g2, int x, int y, int size) {
        int d = Math.min(size, size) - 6;
        int cx = x + (size - d)/2;
        int cy = y + (size - d)/2;
        g2.setColor(Color.BLACK);
        g2.fillOval(cx, cy, d, d);
        g2.setStroke(new BasicStroke(Math.max(2f, d/12f)));
        g2.drawLine(cx + d/2, cy, cx + d, cy - d/3);
    }
}
