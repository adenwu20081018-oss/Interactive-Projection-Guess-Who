package phase5b1;

import com.fazecast.jSerialComm.SerialPort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Standalone Phase 5B-1 controller. One command/reply at a time; no game dependencies. */
public final class ArduinoSerialController implements AutoCloseable {
    public enum Command {
        PING("PONG"), LED_ON("ACK LED_ON"), LED_OFF("ACK LED_OFF"), BLINK("ACK BLINK");
        private final String reply;
        Command(String reply) { this.reply = reply; }
    }

    public record PortInfo(String name, String description) {}

    // Small boundary for hardware-free tests of the actual controller logic.
    interface Connection {
        boolean isOpen();
        int write(byte[] bytes, int offset, int count);
        int read(byte[] bytes);
        boolean close();
    }

    private final Connection connection;
    private final long responseTimeoutMs;
    private boolean closed;
    private boolean ready;

    ArduinoSerialController(Connection connection, long responseTimeoutMs) {
        this.connection = Objects.requireNonNull(connection);
        if (responseTimeoutMs <= 0) throw new IllegalArgumentException("Timeout must be positive.");
        this.responseTimeoutMs = responseTimeoutMs;
    }

    public static List<PortInfo> listPorts() {
        return Arrays.stream(SerialPort.getCommPorts())
                .map(p -> new PortInfo(p.getSystemPortName(), p.getDescriptivePortName()))
                .toList();
    }

    /** Opens only an explicitly selected, currently discovered port. */
    public static ArduinoSerialController open(String portName) throws IOException {
        if (portName == null || portName.isBlank()) throw new IOException("Select a serial port name.");
        SerialPort port = Arrays.stream(SerialPort.getCommPorts())
                .filter(p -> p.getSystemPortName().equalsIgnoreCase(portName.trim()))
                .findFirst().orElseThrow(() -> new IOException("Port " + portName
                        + " was not found. Reconnect the board and list ports again."));
        // Set before opening, then check the active configuration after opening.
        port.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
        if (!port.openPort()) throw new IOException("Cannot open " + portName
                + ". Close Arduino Serial Monitor/Plotter and other serial programs; check the cable.");
        ArduinoSerialController controller = new ArduinoSerialController(new NativeConnection(port), 5000);
        try {
            if (!port.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY)
                    || !port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED)
                    || !port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0)) {
                throw new IOException("Could not configure " + portName + " for 9600 baud, 8N1.");
            }
            controller.initialize(2500);
            return controller;
        } catch (IOException | RuntimeException e) {
            controller.closeAfterFailure(e);
            throw e;
        }
    }

    // Mega may reset when USB serial opens. No startup banner exists in Phase 5A.
    synchronized void initialize(long resetWaitMs) throws IOException {
        try {
            pause(resetWaitMs);
            exchange(Command.PING); // Actual readiness proof, not just a fixed sleep.
            ready = true;
        } catch (IOException e) {
            closeAfterFailure(e);
            throw e;
        }
    }

    public synchronized String send(Command command) throws IOException {
        Objects.requireNonNull(command, "command");
        if (!ready) throw new IOException("Connection is not ready or has been closed. Reopen it.");
        try {
            return exchange(command);
        } catch (IOException e) {
            // Do not reuse a stream with a possibly late or partial response.
            closeAfterFailure(e);
            throw e;
        }
    }

    private String exchange(Command command) throws IOException {
        long deadline = System.nanoTime() + responseTimeoutMs * 1_000_000L;
        byte[] request = (command.name() + "\n").getBytes(StandardCharsets.US_ASCII);
        int offset = 0;
        while (offset < request.length) {
            checkConnectionAndDeadline(deadline, command);
            int written = connection.write(request, offset, request.length - offset);
            if (written < 0) throw new IOException("Serial write failed; the board may be disconnected.");
            offset += written;
            if (written == 0) pause(10);
        }
        StringBuilder response = new StringBuilder();
        byte[] oneByte = new byte[1];
        while (true) {
            checkConnectionAndDeadline(deadline, command);
            int read = connection.read(oneByte);
            if (read < 0) throw new IOException("Serial read failed; the board may be disconnected.");
            if (read == 0) {
                pause(10);
                continue;
            }
            int value = oneByte[0] & 0xff;
            if (value == '\r') continue;
            if (value == '\n') {
                if (response.length() == 0) continue;
                String line = response.toString();
                if (!line.equals(command.reply)) {
                    throw new IOException("Unexpected Arduino response to " + command + ": " + line);
                }
                return line;
            }
            if (value < 32 || value > 126 || response.length() >= 128) {
                throw new IOException("Invalid or oversized serial response. Check baud rate and sketch.");
            }
            response.append((char) value);
        }
    }

    private void checkConnectionAndDeadline(long deadline, Command command) throws IOException {
        if (closed || !connection.isOpen()) throw new IOException("Serial port is closed or disconnected.");
        if (System.nanoTime() - deadline >= 0) {
            throw new IOException("Timed out waiting for " + command + " after " + responseTimeoutMs
                    + " ms. Check the cable, selected port, and Phase 5A sketch; reconnect to retry.");
        }
    }

    private static void pause(long milliseconds) throws IOException {
        try { Thread.sleep(milliseconds); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Serial operation interrupted.", e);
        }
    }

    private void closeAfterFailure(Exception failure) {
        try { close(); }
        catch (IOException closeError) { failure.addSuppressed(closeError); }
    }

    @Override public synchronized void close() throws IOException {
        ready = false;
        if (!closed) {
            if (!connection.close()) throw new IOException("Serial port close failed. Exit and reconnect the board.");
            closed = true;
        }
    }

    private static final class NativeConnection implements Connection {
        private final SerialPort port;
        NativeConnection(SerialPort port) { this.port = port; }
        public boolean isOpen() { return port.isOpen(); }
        public int write(byte[] bytes, int offset, int count) { return port.writeBytes(bytes, count, offset); }
        public int read(byte[] bytes) { return port.readBytes(bytes, bytes.length); }
        public boolean close() { return port.closePort(); }
    }
}
