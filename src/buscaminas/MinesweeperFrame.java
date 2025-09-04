package buscaminas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class MinesweeperFrame extends JFrame {
    private static final String TITLE = "Buscaminas";

    private JPanel boardPanel;
    private JButton[][] buttons;
    private JLabel minesLeftLabel;
    private JComboBox<String> difficultyCombo;
    private JButton newGameButton;

    private MinesweeperGame game;
    private int flagsCount = 0;

    public MinesweeperFrame() {
        super(TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        buildTopBar();
        // Configuración inicial: Principiante
        startNewGame(9, 9, 10);
        setLocationByPlatform(true);
    }

    private void buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.add(new JLabel("Dificultad:"));
        difficultyCombo = new JComboBox<>(new String[]{
                "Principiante (9x9, 10)",
                "Intermedio (16x16, 40)",
                "Experto (16x30, 99)"
        });
        left.add(difficultyCombo);

        newGameButton = new JButton("Nueva partida");
        newGameButton.addActionListener(e -> applySelectedDifficulty());
        left.add(newGameButton);

        top.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        minesLeftLabel = new JLabel("Minas restantes: 0");
        right.add(minesLeftLabel);
        top.add(right, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
    }

    private void applySelectedDifficulty() {
        int[] cfg = getSelectedConfig();
        startNewGame(cfg[0], cfg[1], cfg[2]);
    }

    private int[] getSelectedConfig() {
        String sel = (String) difficultyCombo.getSelectedItem();
        if (sel == null) sel = "Principiante (9x9, 10)";
        if (sel.startsWith("Intermedio")) {
            return new int[]{16, 16, 40};
        } else if (sel.startsWith("Experto")) {
            return new int[]{16, 30, 99};
        } else {
            return new int[]{9, 9, 10};
        }
    }

    private void startNewGame(int rows, int cols, int mines) {
        this.game = new MinesweeperGame(rows, cols, mines);
        game.reset();
        flagsCount = 0;

        if (boardPanel != null) {
            remove(boardPanel);
        }

        boardPanel = new JPanel(new GridLayout(rows, cols, 1, 1));
        boardPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        buttons = new JButton[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JButton b = createCellButton(r, c);
                buttons[r][c] = b;
                boardPanel.add(b);
            }
        }

        add(boardPanel, BorderLayout.CENTER);
        updateMinesLeftLabel();
        packToBoardSize(rows, cols);
        revalidate();
        repaint();
    }

    private void packToBoardSize(int rows, int cols) {
        // Ajusta el tamaño para una experiencia adecuada, limitando tamaño de celda para tableros grandes
        int cell = Math.max(24, Math.min(36, 640 / Math.max(rows, cols)));
        Dimension btnSize = new Dimension(cell, cell);
        for (JButton[] row : buttons) {
            for (JButton b : row) {
                b.setPreferredSize(btnSize);
            }
        }
        pack();
    }

    private JButton createCellButton(int row, int col) {
        JButton b = new JButton();
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setFocusPainted(false);
        b.setBackground(new Color(222, 222, 222));
        b.setOpaque(true);

        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!b.isEnabled()) return;
                if (SwingUtilities.isRightMouseButton(e)) {
                    toggleFlag(b);
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    if ("\u2691".equals(b.getText()) || "\uD83D\uDEA9".equals(b.getText()) || "⚑".equals(b.getText()) || "F".equals(b.getText())) {
                        return; // no abrir si está marcado con bandera
                    }
                    handleOpen(row, col);
                }
            }
        });

        return b;
    }

    private void toggleFlag(JButton b) {
        String t = b.getText();
        if (t == null || t.isEmpty()) {
            b.setText("⚑");
            b.setForeground(new Color(180, 0, 0));
            flagsCount++;
        } else if ("⚑".equals(t)) {
            b.setText("");
            flagsCount--;
        } else {
            // si tiene número visible, no hacer nada
            return;
        }
        updateMinesLeftLabel();
    }

    private void updateMinesLeftLabel() {
        int left = Math.max(0, game.getTotalMines() - flagsCount);
        minesLeftLabel.setText("Minas restantes: " + left);
    }

    private void handleOpen(int row, int col) {
        MinesweeperGame.OpenResult res = game.openCell(row, col);
        if (res.exploded) {
            revealAllMinesAndLose(row, col);
            return;
        }
        updateOpenedCells(res.openedCells);
        if (game.isWin()) {
            handleWin();
        }
    }

    private void updateOpenedCells(List<int[]> opened) {
        for (int[] rc : opened) {
            int r = rc[0], c = rc[1];
            JButton b = buttons[r][c];
            int adj = game.countAdjacentMines(r, c);
            b.setEnabled(false);
            b.setBackground(new Color(240, 240, 240));
            b.setText(adj > 0 ? String.valueOf(adj) : "");
            b.setForeground(colorForNumber(adj));
        }
    }

    private Color colorForNumber(int n) {
        switch (n) {
            case 1: return new Color(0, 102, 204);      // azul
            case 2: return new Color(0, 153, 0);        // verde
            case 3: return new Color(204, 0, 0);        // rojo
            case 4: return new Color(0, 0, 153);        // azul oscuro
            case 5: return new Color(153, 0, 0);        // burdeos
            case 6: return new Color(0, 153, 153);      // teal
            case 7: return Color.BLACK;
            case 8: return Color.GRAY;
            default: return Color.DARK_GRAY;
        }
    }

    private void revealAllMinesAndLose(int clickedR, int clickedC) {
        // Revela todas las minas y desactiva el tablero
        int rows = game.getRows();
        int cols = game.getCols();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JButton b = buttons[r][c];
                b.setEnabled(false);
                if (game.isMine(r, c)) {
                    b.setText("💣");
                    b.setForeground(Color.BLACK);
                    if (r == clickedR && c == clickedC) {
                        b.setBackground(new Color(255, 170, 170));
                    } else {
                        b.setBackground(new Color(250, 210, 210));
                    }
                } else if (!game.isOpened(r, c)) {
                    // opcional: mostrar números restantes
                    int adj = game.countAdjacentMines(r, c);
                    b.setText(adj > 0 ? String.valueOf(adj) : "");
                    b.setForeground(colorForNumber(adj));
                    b.setBackground(new Color(240, 240, 240));
                }
            }
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Has perdido. ¿Quieres jugar otra vez?",
                TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            applySelectedDifficulty();
        }
    }

    private void handleWin() {
        // Desactiva el tablero y felicita
        for (JButton[] row : buttons) {
            for (JButton b : row) b.setEnabled(false);
        }
        JOptionPane.showMessageDialog(this, "¡Has ganado!", TITLE, JOptionPane.INFORMATION_MESSAGE);
        int choice = JOptionPane.showConfirmDialog(this,
                "¿Nueva partida?",
                TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            applySelectedDifficulty();
        }
    }
}

