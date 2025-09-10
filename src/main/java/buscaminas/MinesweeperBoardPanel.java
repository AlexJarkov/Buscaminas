package buscaminas;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLaf;

class MinesweeperBoardPanel extends JComponent {
    private static final long serialVersionUID = 1L;
    interface Listener {
        void onCellsOpened(List<? extends ICell> opened, boolean exploded, int clickedR, int clickedC);
        void onFlagToggled();
    }

    private Color colorCellBg;
    private Color colorCellOpen;
    private Color colorExploded;
    private Color colorMineReveal;
    private Color colorGrid;

    private Color[] numColors;

    private final IMinesweeperGame game;
    @SuppressWarnings("unused")
    private final Listener listener;
    // Base cell size used for text sizing; actual drawing scales to component size
    private int baseCellSize = 28;
    private boolean revealAll = false;
    private int explodedR = -1, explodedC = -1;
    private boolean locked = false;
    private Font numberFont;

    MinesweeperBoardPanel(IMinesweeperGame game, Listener listener) {
        this.game = game;
        this.listener = listener;
        setOpaque(true);
        refreshColors();
        Font base = getFont();
        if (base == null) base = UIManager.getFont("Label.font");
        if (base == null) base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        numberFont = base.deriveFont(Font.BOLD, 14f);

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (locked) return;
                requestFocusInWindow();
                int col = (int) Math.floor(e.getX() / cellWidthF());
                int row = (int) Math.floor(e.getY() / cellHeightF());
                if (col >= game.getCols()) col = game.getCols() - 1;
                if (row >= game.getRows()) row = game.getRows() - 1;
                if (row < 0 || col < 0 || row >= game.getRows() || col >= game.getCols()) return;

                if (SwingUtilities.isRightMouseButton(e)) {
                    if (!game.isOpened(row, col)) {
                        game.toggleFlag(row, col);
                        if (listener != null) listener.onFlagToggled();
                        repaintCell(row, col);
                    }
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    if (game.isFlagged(row, col)) return;
                    IOpenResult res;
                    if (game.isOpened(row, col)) {
                        res = game.chordOpen(row, col);
                    } else {
                        res = game.openCell(row, col);
                    }
                    if (res.exploded()) {
                        revealAllMines(row, col);
                    }
                    if (listener != null) listener.onCellsOpened(res.openedCells(), res.exploded(), row, col);
                    repaint();
                }
            }
        });
    }

    void setLocked(boolean locked) { this.locked = locked; }

    void setCellSize(int cellSize) {
        // Keep for compatibility with existing Frame logic; affects number font size only
        this.baseCellSize = Math.max(16, Math.min(64, cellSize));
        Font base = getFont();
        if (base == null) base = UIManager.getFont("Label.font");
        if (base == null) base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        numberFont = base.deriveFont(Font.BOLD, Math.max(10f, this.baseCellSize * 0.5f));
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
        // Keep a reasonable default size; actual rendering scales to available space
        return new Dimension(game.getCols() * baseCellSize, game.getRows() * baseCellSize);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int rows = game.getRows();
        int cols = game.getCols();
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        double cw = cellWidthF();
        double ch = cellHeightF();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x0 = (int) Math.round(c * cw);
                int y0 = (int) Math.round(r * ch);
                int x1 = (int) Math.round((c + 1) * cw);
                int y1 = (int) Math.round((r + 1) * ch);
                paintCell(g2, r, c, x0, y0, x1 - x0, y1 - y0);
            }
        }

        g2.setColor(colorGrid);
        for (int r = 0; r <= rows; r++) {
            int y = (int) Math.round(r * ch);
            g2.drawLine(0, y, getWidth(), y);
        }
        for (int c = 0; c <= cols; c++) {
            int x = (int) Math.round(c * cw);
            g2.drawLine(x, 0, x, getHeight());
        }
        g2.dispose();
    }

    private void paintCell(Graphics2D g2, int r, int c, int x, int y, int w, int h) {
        if (revealAll && game.isMine(r, c)) {
            g2.setColor((r == explodedR && c == explodedC) ? colorExploded : colorMineReveal);
            g2.fillRect(x, y, w, h);
            int size = Math.min(w, h);
            int ix = x + (w - size) / 2;
            int iy = y + (h - size) / 2;
            paintBomb(g2, ix, iy, size);
            return;
        }

        if (game.isOpened(r, c)) {
            g2.setColor(colorCellOpen);
            g2.fillRect(x, y, w, h);
            int adj = game.countAdjacentMines(r, c);
            if (adj > 0) {
                float fontSize = Math.max(10f, (float) (Math.min(w, h) * 0.5f));
                g2.setFont(numberFont.deriveFont(Font.BOLD, fontSize));
                g2.setColor(colorForNumber(adj));
                String s = Integer.toString(adj);
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (w - fm.stringWidth(s)) / 2;
                int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(s, tx, ty);
            }
        } else {
            g2.setColor(colorCellBg);
            g2.fillRect(x, y, w, h);
            if (game.isFlagged(r, c)) {
                int size = Math.min(w, h);
                int ix = x + (w - size) / 2;
                int iy = y + (h - size) / 2;
                paintFlag(g2, ix, iy, size);
            }
        }
    }

    private void repaintCell(int r, int c) {
        int x0 = (int) Math.round(c * cellWidthF());
        int y0 = (int) Math.round(r * cellHeightF());
        int x1 = (int) Math.round((c + 1) * cellWidthF());
        int y1 = (int) Math.round((r + 1) * cellHeightF());
        repaint(x0, y0, x1 - x0, y1 - y0);
    }

    private Color colorForNumber(int n) { return numColors[Math.max(0, Math.min(8, n))]; }

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
    private double cellWidthF() { return getWidth() / (double) game.getCols(); }
    private double cellHeightF() { return getHeight() / (double) game.getRows(); }

    @Override public void updateUI() {
        super.updateUI();
        refreshColors();
    }

    private void refreshColors() {
        boolean dark = FlatLaf.isLafDark();
        if (dark) {
            setBackground(UIManager.getColor("Panel.background") != null ? UIManager.getColor("Panel.background") : new Color(34, 34, 38));
            colorCellBg = new Color(58, 58, 62);
            colorCellOpen = new Color(68, 68, 72);
            colorExploded = new Color(130, 55, 55);
            colorMineReveal = new Color(100, 50, 50);
            colorGrid = new Color(90, 90, 96);
            numColors = new Color[]{
                    new Color(220, 220, 220), // 0
                    new Color(100, 170, 255), // 1
                    new Color(90, 200, 90),   // 2
                    new Color(255, 120, 120), // 3
                    new Color(120, 140, 255), // 4
                    new Color(255, 140, 140), // 5
                    new Color(120, 220, 220), // 6
                    new Color(230, 230, 230), // 7
                    new Color(190, 190, 190)  // 8
            };
        } else {
            setBackground(new Color(200, 200, 200));
            colorCellBg = new Color(222, 222, 222);
            colorCellOpen = new Color(240, 240, 240);
            colorExploded = new Color(255, 170, 170);
            colorMineReveal = new Color(250, 210, 210);
            colorGrid = new Color(180, 180, 180);
            numColors = new Color[]{
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
        }
        repaint();
    }
}
