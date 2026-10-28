package groupDGuessWho;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import phase5b1.ArduinoSerialController;

/** Owns one connection on one background thread. Never waits for serial on the EDT. */
final class ArduinoFeedback implements AutoCloseable {
    interface Session extends AutoCloseable {
        String blink() throws IOException;
        void close() throws IOException;
    }
    interface Connector { Session open(String port) throws IOException; }

    private final ArrayBlockingQueue<String> pending = new ArrayBlockingQueue<>(16);
    private final Thread worker;
    private volatile boolean closed;
    private volatile boolean ready;

    ArduinoFeedback(String port) {
        this(port, name -> {
            ArduinoSerialController controller = ArduinoSerialController.open(name);
            return new Session() {
                public String blink() throws IOException {
                    return controller.send(ArduinoSerialController.Command.BLINK);
                }
                public void close() throws IOException { controller.close(); }
            };
        });
    }

    ArduinoFeedback(String port, Connector connector) {
        worker = new Thread(() -> run(port, connector), "guess-who-arduino");
        worker.setDaemon(true);
        worker.start();
    }

    void actionCompleted(String action) {
        System.out.println("GAME action completed: " + action);
        if (closed || !ready) {
            System.out.println("ARDUINO unavailable: feedback skipped (connecting, disabled, or disconnected).");
        } else if (!pending.offer(action)) {
            System.out.println("ARDUINO feedback queue full: skipped " + action);
        }
    }

    private void run(String port, Connector connector) {
        Session session = null;
        try {
            System.out.println("ARDUINO connecting on " + port + "; waiting for reset and PONG...");
            session = connector.open(port); // Reuses Phase 5B-1's reset wait and handshake.
            if (closed) return;
            ready = true;
            System.out.println("ARDUINO connected on " + port + " at 9600 baud, 8N1; response received: PONG");
            while (!closed) {
                String action = pending.take();
                if (closed) break;
                System.out.println("ARDUINO sending BLINK for " + action);
                String reply = session.blink();
                System.out.println("ARDUINO BLINK command sent; response received: " + reply);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException | LinkageError e) {
            if (!closed) {
                System.err.println("ARDUINO serial communication error: " + e.getMessage());
                System.err.println("ARDUINO unavailable: game continues without feedback. Restart game to reconnect.");
            }
        } finally {
            ready = false;
            pending.clear();
            if (session != null) {
                try { session.close(); }
                catch (IOException | RuntimeException | LinkageError e) {
                    System.err.println("ARDUINO close error: " + e.getMessage());
                }
                System.out.println("ARDUINO disconnected; serial connection closed or cleanup attempted.");
            }
        }
    }

    boolean isReady() { return ready; }

    /** Safe on EDT: request shutdown only; worker closes its own connection. */
    @Override public void close() {
        closed = true;
        ready = false;
        pending.clear();
        worker.interrupt();
    }

    // Called by the JVM shutdown hook (not the EDT), also useful in tests.
    void awaitClosed() {
        try { worker.join(TimeUnit.SECONDS.toMillis(8)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        if (worker.isAlive()) System.err.println("ARDUINO cleanup still pending at shutdown.");
    }
}
