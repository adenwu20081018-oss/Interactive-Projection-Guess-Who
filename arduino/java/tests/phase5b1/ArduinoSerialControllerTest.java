package phase5b1;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Hardware-free regression tests. Uses the real controller with an in-memory connection. */
public final class ArduinoSerialControllerTest {
    private static int passed;
    public static void main(String[] args) throws Exception {
        Fake f = new Fake();
        try (var c = ready(f, 500)) {
            for (var command : ArduinoSerialController.Command.values()) c.send(command);
            check(f.commands.equals(List.of("PING\n", "PING\n", "LED_ON\n", "LED_OFF\n", "BLINK\n")),
                    "startup handshake and four exact newline-terminated commands with CRLF replies");
        }
        check(f.closeCount == 1, "normal close");

        f = new Fake(); f.maxWrite = 1;
        try (var c = ready(f, 500)) {
            check(c.send(ArduinoSerialController.Command.LED_ON).equals("ACK LED_ON"), "partial writes");
        }

        f = new Fake();
        try (var c = ready(f, 500)) {
            f.replyOverride = "\nPONG\n";
            check(c.send(ArduinoSerialController.Command.PING).equals("PONG"), "LF replies and blank lines");
            f.replyOverride = null; f.delayMs = 80;
            long start = System.nanoTime();
            c.send(ArduinoSerialController.Command.BLINK);
            check(System.nanoTime() - start >= 80_000_000L, "wait for delayed BLINK acknowledgement");
        }

        assertFailure("no reply times out", fake -> fake.silent = true, "Timed out");
        assertFailure("partial reply times out", fake -> fake.replyOverride = "PO", "Timed out");
        assertFailure("Arduino error displayed", fake -> fake.replyOverride = "ERR UNKNOWN_COMMAND\r\n", "ERR UNKNOWN_COMMAND");
        assertFailure("wrong acknowledgement rejected", fake -> fake.replyOverride = "ACK LED_ON\n", "Unexpected");
        assertFailure("oversized reply rejected", fake -> fake.replyOverride = "X".repeat(129) + "\n", "oversized");
        assertFailure("invalid byte rejected", fake -> fake.replyOverride = "PO\0NG\n", "Invalid");
        assertFailure("disconnection detected", fake -> fake.open = false, "disconnected");
        assertFailure("native read error handled", fake -> fake.readError = true, "read failed");
        assertFailure("native write error handled", fake -> fake.writeError = true, "write failed");
        assertFailure("stalled write times out", fake -> fake.maxWrite = 0, "Timed out");

        f = new Fake(); f.silent = true;
        var c = new ArduinoSerialController(f, 60);
        var startupController = c;
        expectFailure(() -> startupController.initialize(0), "Timed out");
        check(f.closeCount == 1, "failed startup handshake closes connection");

        f = new Fake(); c = new ArduinoSerialController(f, 500);
        long start = System.nanoTime();
        c.initialize(40);
        check(f.firstWriteNanos - start >= 40_000_000L, "reset wait precedes first command");
        c.close(); c.close();
        check(f.closeCount == 1, "close is idempotent");
        var closedController = c;
        expectFailure(() -> closedController.send(ArduinoSerialController.Command.PING), "not ready");
        check(f.commands.size() == 1, "closed connection cannot send");

        f = new Fake(); c = ready(f, 500);
        var interruptedController = c;
        f.silent = true;
        Thread.currentThread().interrupt();
        expectFailure(() -> interruptedController.send(ArduinoSerialController.Command.PING), "interrupted");
        check(Thread.interrupted() && f.closeCount == 1, "interruption preserves flag and closes connection");
        System.out.println("PASS: " + passed + " hardware-free checks");
    }

    private static ArduinoSerialController ready(Fake f, long timeout) throws IOException {
        var c = new ArduinoSerialController(f, timeout);
        c.initialize(0);
        return c;
    }
    interface Action { void run() throws IOException; }
    private static void expectFailure(Action action, String text) throws IOException {
        try { action.run(); }
        catch (IOException e) {
            if (!e.getMessage().contains(text)) throw new AssertionError(e);
            return;
        }
        throw new AssertionError("Expected IOException containing: " + text);
    }
    private static void assertFailure(String name, java.util.function.Consumer<Fake> configure, String text)
            throws IOException {
        Fake f = new Fake();
        var c = ready(f, 60);
        configure.accept(f);
        expectFailure(() -> c.send(ArduinoSerialController.Command.PING), text);
        check(f.closeCount == 1, name + " and closes connection");
        expectFailure(() -> c.send(ArduinoSerialController.Command.PING), "not ready");
        c.close();
    }
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        passed++;
        System.out.println("PASS " + name);
    }

    private static final class Fake implements ArduinoSerialController.Connection {
        boolean open = true, silent, readError, writeError;
        int closeCount, maxWrite = Integer.MAX_VALUE;
        long delayMs, availableAt, firstWriteNanos;
        String replyOverride;
        final StringBuilder pending = new StringBuilder();
        final ArrayDeque<Byte> incoming = new ArrayDeque<>();
        final List<String> commands = new ArrayList<>();
        public boolean isOpen() { return open; }
        public int write(byte[] bytes, int offset, int count) {
            if (writeError) return -1;
            if (firstWriteNanos == 0) firstWriteNanos = System.nanoTime();
            int n = Math.min(count, maxWrite);
            for (int i = offset; i < offset + n; i++) {
                pending.append((char) bytes[i]);
                if (bytes[i] == '\n') {
                    String request = pending.toString();
                    commands.add(request);
                    pending.setLength(0);
                    String reply = replyOverride != null ? replyOverride : switch (request) {
                        case "PING\n" -> "PONG\r\n";
                        case "LED_ON\n" -> "ACK LED_ON\r\n";
                        case "LED_OFF\n" -> "ACK LED_OFF\r\n";
                        case "BLINK\n" -> "ACK BLINK\r\n";
                        default -> "ERR UNKNOWN_COMMAND\r\n";
                    };
                    if (!silent) for (byte b : reply.getBytes(StandardCharsets.US_ASCII)) incoming.add(b);
                    availableAt = System.nanoTime() + delayMs * 1_000_000L;
                }
            }
            return n;
        }
        public int read(byte[] bytes) {
            if (readError) return -1;
            if (incoming.isEmpty() || System.nanoTime() < availableAt) return 0;
            bytes[0] = incoming.remove();
            return 1;
        }
        public boolean close() { open = false; closeCount++; return true; }
    }
}
