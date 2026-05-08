package EndGameDatabase.dataClasses;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class AtomicByteArray {

    private static final VarHandle BYTE_HANDLE =
        MethodHandles.arrayElementVarHandle(byte[].class);

    private final byte[] array;
    private final int length;

    public AtomicByteArray(int length) {
        if (length < 0) throw new IllegalArgumentException("Length must be >= 0");
        this.length = length;
        this.array = new byte[length];
    }

    public int get(int index) {
        checkIndex(index);
        return (int) BYTE_HANDLE.get(array, index) & 0xFF;   // return as unsigned 0-255
    }

    public void set(int index, int value) {
        checkIndex(index);
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Value must be between 0 and 255");
        }
        BYTE_HANDLE.set(array, index, (byte) value);
    }

    public int getAndSet(int index, int newValue) {
        checkIndex(index);
        if (newValue < 0 || newValue > 255)
            throw new IllegalArgumentException("Value must be 0-255");

        byte old = (byte) BYTE_HANDLE.getAndSet(array, index, (byte) newValue);
        return old & 0xFF;
    }

    public boolean compareAndSet(int index, int expected, int update) {
        checkIndex(index);
        if (expected < 0 || expected > 255 || update < 0 || update > 255)
            throw new IllegalArgumentException();

        return BYTE_HANDLE.compareAndSet(array, index,
                                        (byte) expected, (byte) update);
    }

    // Useful atomic operations
    public int getAndIncrement(int index) {
        checkIndex(index);
        byte old = (byte) BYTE_HANDLE.getAndAdd(array, index, (byte) 1);
        return old & 0xFF;
    }

    public int getAndAdd(int index, int delta) {
        checkIndex(index);
        byte old = (byte) BYTE_HANDLE.getAndAdd(array, index, (byte) delta);
        return old & 0xFF;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(index);
        }
    }

    public int length() {
        return length;
    }
}
