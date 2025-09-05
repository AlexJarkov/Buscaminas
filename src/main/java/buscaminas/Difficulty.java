package buscaminas;

public enum Difficulty {
    BEGINNER("Principiante (9x9, 10)", 9, 9, 10),
    INTERMEDIATE("Intermedio (16x16, 40)", 16, 16, 40),
    EXPERT("Experto (16x30, 99)", 16, 30, 99);

    private final String label;
    public final int rows;
    public final int cols;
    public final int mines;

    Difficulty(String label, int rows, int cols, int mines) {
        this.label = label;
        this.rows = rows;
        this.cols = cols;
        this.mines = mines;
    }

    @Override
    public String toString() {
        return label;
    }
}

