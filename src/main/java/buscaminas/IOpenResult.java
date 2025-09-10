package buscaminas;

import java.util.List;

/**
 * Result of an open/chord action.
 */
public interface IOpenResult {
    boolean exploded();
    List<? extends ICell> openedCells();
}

