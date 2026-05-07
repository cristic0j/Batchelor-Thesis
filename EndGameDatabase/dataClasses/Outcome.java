package EndGameDatabase.dataClasses;

public enum Outcome {
    LOSS(0), WIN(1), DRAW(2), NON_CANONICAL_INDEX(3);

    public final int value;

    Outcome(int value) { this.value = value; }
}
