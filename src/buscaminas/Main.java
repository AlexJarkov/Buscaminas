package buscaminas;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MinesweeperFrame frame = new MinesweeperFrame();
            frame.setVisible(true);
        });
    }
}

