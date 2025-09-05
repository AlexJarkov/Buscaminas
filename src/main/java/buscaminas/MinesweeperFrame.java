package buscaminas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

public class MinesweeperFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String TITLE = "Buscaminas";
    private static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");

    private MinesweeperBoardPanel boardPanel;
    private JPanel boardContainer;
    private JPanel topBar;
    private JLabel minesLeftLabel;
    private JComboBox<Difficulty> difficultyCombo;
    private JButton newGameButton;

    private MinesweeperGame game;
    private int currentCellSize = 28;

    public MinesweeperFrame() {
        super(TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        applyPlatformWindowStyling();
        setLayout(new BorderLayout());
        buildTopBar();
        boardContainer = new JPanel(new BorderLayout());
        add(boardContainer, BorderLayout.CENTER);
        // Configuración inicial: Principiante
        Difficulty initial = Difficulty.BEGINNER;
        startNewGame(initial.rows, initial.cols, initial.mines);
        setLocationByPlatform(true);
    }

    private void buildTopBar() {
        topBar = new JPanel(new GridBagLayout());
        // Add extra top padding on macOS when using a transparent title bar
        int topPad = IS_MAC ? 28 : 8;
        topBar.setBorder(new EmptyBorder(topPad, 8, 8, 8));
        // With FlatLaf, match title bar background for a unified toolbar look
        topBar.putClientProperty("FlatLaf.style", "background: @TitlePane.background;" +
                "border: 0,0,1,0; borderColor: @TitlePane.borderColor");

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.add(new JLabel("Dificultad:"));
        difficultyCombo = new JComboBox<>(new DefaultComboBoxModel<>(Difficulty.values()));
        controls.add(difficultyCombo);

        newGameButton = new JButton("Nueva partida");
        // Use round-rect button style on macOS/FlatLaf for a native vibe
        newGameButton.putClientProperty("JButton.buttonType", "roundRect");
        newGameButton.addActionListener(e -> applySelectedDifficulty());
        controls.add(newGameButton);

        minesLeftLabel = new JLabel("Minas restantes: 0");

        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.gridx = 0; gbcLeft.gridy = 0; gbcLeft.weightx = 1.0; gbcLeft.fill = GridBagConstraints.HORIZONTAL; gbcLeft.anchor = GridBagConstraints.WEST;
        gbcLeft.insets = new Insets(0,0,0,8);
        topBar.add(controls, gbcLeft);

        GridBagConstraints gbcRight = new GridBagConstraints();
        gbcRight.gridx = 1; gbcRight.gridy = 0; gbcRight.weightx = 0; gbcRight.anchor = GridBagConstraints.EAST;
        topBar.add(minesLeftLabel, gbcRight);

        topBar.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { updateTopBarWrap(); }
            @Override public void componentShown(ComponentEvent e) { updateTopBarWrap(); }
        });

        add(topBar, BorderLayout.NORTH);
    }

    private void applyPlatformWindowStyling() {
        if (!IS_MAC) return;
        // Unify content with title bar for a native macOS look
        JRootPane root = getRootPane();
        root.putClientProperty("apple.awt.fullWindowContent", Boolean.TRUE);
        root.putClientProperty("apple.awt.transparentTitleBar", Boolean.TRUE);
        root.putClientProperty("apple.awt.windowTitleVisible", Boolean.FALSE);
        // Allow native full-screen (green traffic light)
        root.putClientProperty("apple.awt.fullscreenable", Boolean.TRUE);
    }

    private void updateTopBarWrap() {
        if (topBar == null) return;
        GridBagLayout layout = (GridBagLayout) topBar.getLayout();
        Component controls = topBar.getComponent(0);
        Insets in = topBar.getInsets();
        int available = Math.max(0, topBar.getWidth() - (in.left + in.right));
        int needed = controls.getPreferredSize().width + minesLeftLabel.getPreferredSize().width + 16;
        boolean collapse = available > 0 && needed > available;

        GridBagConstraints gbcLeft = layout.getConstraints(controls);
        GridBagConstraints gbcRight = layout.getConstraints(minesLeftLabel);
        gbcLeft.gridx = 0; gbcLeft.gridy = 0; gbcLeft.weightx = 1.0; gbcLeft.fill = GridBagConstraints.HORIZONTAL; gbcLeft.anchor = GridBagConstraints.WEST;
        if (collapse) {
            gbcRight.gridx = 0; gbcRight.gridy = 1; gbcRight.weightx = 1.0; gbcRight.fill = GridBagConstraints.HORIZONTAL; gbcRight.anchor = GridBagConstraints.WEST;
            gbcRight.insets = new Insets(4, 0, 0, 0);
        } else {
            gbcRight.gridx = 1; gbcRight.gridy = 0; gbcRight.weightx = 0.0; gbcRight.fill = GridBagConstraints.NONE; gbcRight.anchor = GridBagConstraints.EAST;
            gbcRight.insets = new Insets(0, 0, 0, 0);
        }
        layout.setConstraints(controls, gbcLeft);
        layout.setConstraints(minesLeftLabel, gbcRight);
        topBar.revalidate();
        topBar.repaint();
    }

    private void applySelectedDifficulty() {
        Difficulty d = (Difficulty) difficultyCombo.getSelectedItem();
        if (d == null) d = Difficulty.BEGINNER;
        startNewGame(d.rows, d.cols, d.mines);
    }

    private void startNewGame(int rows, int cols, int mines) {
        this.game = new MinesweeperGame(rows, cols, mines);

        if (boardPanel != null) boardContainer.remove(boardPanel);

        boardPanel = new MinesweeperBoardPanel(game, new MinesweeperBoardPanel.Listener() {
            @Override public void onCellsOpened(List<MinesweeperGame.Cell> opened, boolean exploded, int clickedR, int clickedC) {
                updateMinesLeftLabel();
                if (exploded) {
                    revealAllMinesAndLose(clickedR, clickedC);
                    return;
                }
                if (game.isWin()) handleWin();
            }

            @Override public void onFlagToggled() { updateMinesLeftLabel(); }
        });
        JPanel wrap = new JPanel(new BorderLayout());
        // Remove extra insets so the grid uses all available space
        wrap.setBorder(new EmptyBorder(0, 0, 0, 0));
        wrap.add(boardPanel, BorderLayout.CENTER);
        boardContainer.removeAll();
        boardContainer.add(wrap, BorderLayout.CENTER);
        boardContainer.setOpaque(true);
        boardContainer.setBackground(boardPanel.getBackground());
        updateMinesLeftLabel();
        packToBoardSize(rows, cols);
        if (boardContainer != null && boardContainer.getComponentListeners().length == 0) {
            boardContainer.addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) { recomputeCellSizeToFit(); }
                @Override public void componentShown(ComponentEvent e) { recomputeCellSizeToFit(); }
            });
        }
        SwingUtilities.invokeLater(this::recomputeCellSizeToFit);
        revalidate();
        repaint();
    }

    private void packToBoardSize(int rows, int cols) {
        // Fija el tamaño de celda inicial en función de las filas para que
        // Modo Intermedio (16x16) y Modo Experto (16x30) usen el mismo tamaño.
        int cell = Math.max(24, Math.min(36, 640 / rows));
        currentCellSize = cell;
        boardPanel.setCellSize(cell);
        boardPanel.setPreferredSize(new Dimension(cols * cell, rows * cell));
        pack();
    }

    private void recomputeCellSizeToFit() {
        if (boardPanel == null || game == null) return;
        int rows = game.getRows(), cols = game.getCols();
        Dimension size = boardContainer.getSize();
        if (size.width <= 0 || size.height <= 0) return;
        int usableW = Math.max(0, size.width);
        int usableH = Math.max(0, size.height);
        int cw = usableW / cols;
        int ch = usableH / rows;
        int cell = Math.max(16, Math.min(64, Math.min(cw, ch)));
        if (cell != currentCellSize) {
            currentCellSize = cell;
            boardPanel.setCellSize(cell);
            boardPanel.repaint();
        }
    }

    private void updateMinesLeftLabel() {
        int left = Math.max(0, game.getTotalMines() - game.getFlagsCount());
        minesLeftLabel.setText("Minas restantes: " + left);
    }

    

    private void revealAllMinesAndLose(int clickedR, int clickedC) {
        boardPanel.revealAllMines(clickedR, clickedC);

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
        boardPanel.setLocked(true);
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
