package dataClasses;
import java.util.Set;

public enum BoardBorder {
    TOP_ROW(Set.of(0, 1, 2, 3, 4, 5)),
    BOTTOM_ROW(Set.of(30, 31, 32, 33, 34, 35)),
    RIGHT_BORDER(Set.of(5, 11, 17, 23, 29, 35)),
    LEFT_BORDER(Set.of(0, 6, 12, 18, 24, 30));

    private final Set<Integer> spaces;

    // The constructor that was missing
    BoardBorder(Set<Integer> spaces) {
        this.spaces = spaces;
    }

    
    // Check if it's on ANY border
    public static boolean isOnAnyBorder(int coordinate) {
        for (BoardBorder border : values()) {
            if (border.spaces.contains(coordinate)) return true;
        }
        return false;
    }

    // Specific Border Checks
    public static boolean isTop(int coordinate) {
        return TOP_ROW.spaces.contains(coordinate);
    }

    public static boolean isBottom(int coordinate) {
        return BOTTOM_ROW.spaces.contains(coordinate);
    }

    public static boolean isLeft(int coordinate) {
        return LEFT_BORDER.spaces.contains(coordinate);
    }

    public static boolean isRight(int coordinate) {
        return RIGHT_BORDER.spaces.contains(coordinate);
    }
}