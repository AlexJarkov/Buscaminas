package buscaminas;

/**
 * Minimal cell coordinate interface to decouple view from implementation.
 */
public interface ICell {
    int r();
    int c();
}

