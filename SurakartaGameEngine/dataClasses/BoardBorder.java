package dataClasses;

public enum BoardBorder {
    TOP_ROW,
    BOTTOM_ROW,
    RIGHT_BORDER,
    LEFT_BORDER;

    // boolean[] lookup tables — O(1) array access, zero boxing
    private static final boolean[] ON_TOP    = new boolean[36];
    private static final boolean[] ON_BOTTOM = new boolean[36];
    private static final boolean[] ON_LEFT   = new boolean[36];
    private static final boolean[] ON_RIGHT  = new boolean[36];
    private static final boolean[] ON_ANY    = new boolean[36];

    static {
        for (int i = 0;  i < 6;  i++) { ON_TOP[i]        = true; ON_ANY[i]    = true; }
        for (int i = 30; i < 36; i++) { ON_BOTTOM[i]     = true; ON_ANY[i]    = true; }
        for (int i = 0;  i < 36; i += 6) { ON_LEFT[i]   = true; ON_ANY[i]    = true; }
        for (int i = 5;  i < 36; i += 6) { ON_RIGHT[i]  = true; ON_ANY[i]    = true; }
    }

    public static boolean isOnAnyBorder(int coordinate) { return ON_ANY[coordinate];    }
    public static boolean isTop(int coordinate)         { return ON_TOP[coordinate];    }
    public static boolean isBottom(int coordinate)      { return ON_BOTTOM[coordinate]; }
    public static boolean isLeft(int coordinate)        { return ON_LEFT[coordinate];   }
    public static boolean isRight(int coordinate)       { return ON_RIGHT[coordinate];  }
}