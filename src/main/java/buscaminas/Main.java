package buscaminas;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
// no external OS detector; we do a lightweight macOS check

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Main {
    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    private static void setupModernMacLook() {
        // macOS: move menu bar to the top and set app name
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "Buscaminas");
        System.setProperty("apple.awt.application.appearance", "system");

        // Use FlatLaf macOS theme (Big Sur/Monterey style)
        try {
            boolean dark = prefersDark();
            FlatAnimatedLafChange.showSnapshot();
            if (dark) FlatMacDarkLaf.setup(); else FlatMacLightLaf.setup();
            FlatLaf.updateUI();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        } catch (Throwable t) {
            // Fallback to the system LAF if FlatLaf is not present
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
                // keep default
            }
        }
    }

    private static boolean prefersDark() {
        String force = System.getProperty("app.theme", "");
        if ("dark".equalsIgnoreCase(force)) return true;
        if ("light".equalsIgnoreCase(force)) return false;
        // macOS detection
        String prop = System.getProperty("apple.awt.application.appearance", "");
        if (prop != null && prop.toLowerCase().contains("dark")) return true;
        // Try reading global AppleInterfaceStyle; returns "Dark" when dark mode is enabled
        try {
            Process p = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle").redirectErrorStream(true).start();
            try (java.io.InputStream in = p.getInputStream(); java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A")) {
                String out = s.hasNext() ? s.next().trim() : "";
                return out.equalsIgnoreCase("Dark");
            }
        } catch (java.io.IOException | SecurityException ignored) {
            return false;
        }
    }

    private static void setNativeLookAndFeel() {
        if (isMac()) {
            setupModernMacLook();
            return;
        }

        // Non-macOS: use the system LAF with fallback
        JFrame.setDefaultLookAndFeelDecorated(false);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
                // Keep default
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setNativeLookAndFeel();
            startThemeWatcher();
            MinesweeperFrame frame = new MinesweeperFrame();
            frame.setVisible(true);
        });
    }

    private static void startThemeWatcher() {
        // Allow forcing theme; skip watching if explicitly set
        String force = System.getProperty("app.theme", "");
        if (!force.isEmpty()) return;

        // Only macOS is supported for live OS theme detection here
        if (!isMac()) return;

        final boolean[] lastDark = { FlatLaf.isLafDark() };
        Timer t = new Timer(2000, e -> {
            boolean nowDark = prefersDark();
            if (nowDark != lastDark[0]) {
                lastDark[0] = nowDark;
                FlatAnimatedLafChange.showSnapshot();
                if (nowDark) FlatMacDarkLaf.setup(); else FlatMacLightLaf.setup();
                FlatLaf.updateUI();
                FlatAnimatedLafChange.hideSnapshotWithAnimation();
            }
        });
        t.setRepeats(true);
        t.start();
    }
}
