package phase5b1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/** Run without the Guess Who GUI. Optional argument: a port name or --list. */
public final class ArduinoSerialTest {
    public static void main(String[] args) {
        int status = run(args);
        if (status != 0) System.exit(status);
    }

    private static int run(String[] args) {
        if (args.length > 1 || (args.length == 1 && args[0].equals("--help"))) {
            System.out.println("Usage: phase5b1.ArduinoSerialTest [COM-port | --list]");
            return args.length > 1 ? 2 : 0;
        }
        AtomicReference<ArduinoSerialController> active = new AtomicReference<>();
        Thread shutdownHook = new Thread(() -> {
            ArduinoSerialController controller = active.get();
            if (controller != null) {
                try { controller.close(); }
                catch (IOException e) { System.err.println("Close error: " + e.getMessage()); }
            }
        }, "arduino-serial-close");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            var ports = ArduinoSerialController.listPorts();
            System.out.println("Available serial ports:");
            for (var port : ports) System.out.println("  " + port.name() + " - " + port.description());
            if (ports.isEmpty()) System.out.println("  (none detected; connect the board and check Device Manager)");
            if (args.length == 1 && args[0].equals("--list")) return 0;
            if (ports.isEmpty()) return 1;
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            String selected;
            if (args.length == 1) selected = args[0];
            else {
                System.out.print("Enter port name (for example COM5), or QUIT: ");
                selected = console.readLine();
                if (selected == null || selected.trim().equalsIgnoreCase("QUIT")) return 0;
            }
            System.out.println("Close Arduino Serial Monitor/Plotter before continuing.");
            System.out.println("Opening " + selected + "; waiting for Mega reset, then PING -> PONG...");
            try (ArduinoSerialController controller = ArduinoSerialController.open(selected)) {
                active.set(controller);
                System.out.println("Arduino: PONG (startup handshake). Ready at 9600 baud, 8N1.");
                System.out.println("Commands: PING, LED_ON, LED_OFF, BLINK, ALL, QUIT.");
                System.out.println("ALL runs the four commands in that order; wait for each response.");
                while (true) {
                    System.out.print("> ");
                    String input = console.readLine();
                    if (input == null || input.trim().equalsIgnoreCase("QUIT")) break;
                    input = input.trim().toUpperCase(Locale.ROOT);
                    if (input.isEmpty()) continue;
                    if (input.equals("ALL")) {
                        for (var command : ArduinoSerialController.Command.values()) {
                            transmit(controller, command);
                            if (command == ArduinoSerialController.Command.LED_ON) Thread.sleep(1000);
                        }
                    } else {
                        ArduinoSerialController.Command command;
                        try { command = ArduinoSerialController.Command.valueOf(input); }
                        catch (IllegalArgumentException e) {
                            System.out.println("Unknown command. Use PING, LED_ON, LED_OFF, BLINK, ALL, or QUIT.");
                            continue;
                        }
                        transmit(controller, command);
                    }
                }
            }
            active.set(null);
            System.out.println("Serial port closed.");
            return 0;
        } catch (IOException | RuntimeException | LinkageError e) {
            System.err.println("Communication error: " + e.getMessage());
            for (Throwable suppressed : e.getSuppressed()) System.err.println("Close error: " + suppressed.getMessage());
            return 1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Test interrupted; closing the serial port.");
            return 1;
        } finally {
            // Retain the shutdown hook if a failed close needs another attempt on exit.
            if (active.get() == null) Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
    }

    private static void transmit(ArduinoSerialController controller,
                                 ArduinoSerialController.Command command) throws IOException {
        System.out.println("Java -> " + command);
        System.out.println("Arduino -> " + controller.send(command));
    }
}
