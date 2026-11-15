package dev.vatori.ipcframework;

/** Deterministic payload/limit contract, not a substitute for real streaming. */
public final class FlowTransferEnduranceFixtureSmokeMain {
    public static void main(String[] args) {
        for (int size : new int[]{4 * 1024 * 1024, 256 * 1024 * 1024}) {
            if (FlowTransferEnduranceRoute.checkedSize(Integer.toString(size)) != size)
                throw new AssertionError("accepted size");
            for (int seed : new int[]{0, 17, 127, 255}) {
                for (long offset : new long[]{0, 1, 255, 256, 32767, 32768, size - 1L}) {
                    int expected = (int) ((offset * 31 + seed) & 255);
                    if (FlowTransferEnduranceRoute.expected(offset, seed) != expected)
                        throw new AssertionError("payload pattern");
                }
            }
        }
        for (String value : new String[]{null, "", "-1", "0", "4194303", "268435457", "2147483648"}) {
            try {
                FlowTransferEnduranceRoute.checkedSize(value);
                throw new AssertionError("unexpected size accepted: " + value);
            } catch (IllegalArgumentException expected) { }
        }
        System.out.println("PASS: endurance payload boundary/size contract (no runtime claim)");
    }
}
