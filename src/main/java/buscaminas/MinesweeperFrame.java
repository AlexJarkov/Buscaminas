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
        topBar.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.add(new JLabel("Dificultad:"));
        difficultyCombo = new JComboBox<>(new DefaultComboBoxModel<>(Difficulty.values()));
        controls.add(difficultyCombo);

        newGameButton = new JButton("Nueva partida");
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
        wrap.setBorder(new EmptyBorder(8, 8, 8, 8));
        wrap.add(boardPanel, BorderLayout.CENTER);
        boardContainer.removeAll();
        boardContainer.add(wrap, BorderLayout.CENTER);
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
        // Ajusta el tamaño para una experiencia adecuada, limitando tamaño de celda para tableros grandes
        int cell = Math.max(24, Math.min(36, 640 / Math.max(rows, cols)));
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
        int usableW = Math.max(0, size.width - 16);
        int usableH = Math.max(0, size.height - 16);
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
