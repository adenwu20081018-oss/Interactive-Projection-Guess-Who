package groupDGuessWho;

public class Main {
    public static void main(String[] args) throws Exception {
        int port=5050, width=1280, height=720;
        boolean enabled=true, contain=false;
        boolean presentation=false, listDisplays=false;
        int display=-1, windowWidth=1000, windowHeight=625;
        String arduinoPort=null;
        try {
            for (int i=0;i<args.length;i++) {
                switch(args[i]) {
                    case "--presentation": presentation=true; break;
                    case "--list-displays": listDisplays=true; break;
                    case "--display": display=Integer.parseInt(args[++i]); break;
                    case "--window-width": windowWidth=Integer.parseInt(args[++i]); presentation=true; break;
                    case "--window-height": windowHeight=Integer.parseInt(args[++i]); presentation=true; break;
                    case "--arduino-port":
                        arduinoPort=args[++i];
                        if (arduinoPort.isBlank() || arduinoPort.startsWith("--"))
                            throw new IllegalArgumentException("Provide a serial port such as COM5");
                        break;
                    case "--no-control": enabled=false; break;
                    case "--control-port": port=Integer.parseInt(args[++i]); break;
                    case "--viewport-width": width=Integer.parseInt(args[++i]); break;
                    case "--viewport-height": height=Integer.parseInt(args[++i]); break;
                    case "--viewport-fit":
                        String fit=args[++i];
                        if (!fit.equals("stretch") && !fit.equals("contain")) throw new IllegalArgumentException("fit must be stretch or contain");
                        contain=fit.equals("contain"); break;
                    default: throw new IllegalArgumentException("Unknown option: "+args[i]);
                }
            }
            if (port<1 || port>65535 || width<2 || height<2 || width>16384 || height>16384)
                throw new IllegalArgumentException("Invalid port or viewport dimensions");
            if (display < -1 || windowWidth < 800 || windowHeight < 500 || windowWidth > 7680 || windowHeight > 4320)
                throw new IllegalArgumentException("Window must be 800..7680 by 500..4320; display must be nonnegative");
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            System.err.println("Usage: Main [--presentation] [--window-width 1000 --window-height 625] [--list-displays] [--display 0] [--arduino-port COM5] [--control-port 5050] [--viewport-width 1280] [--viewport-height 720] [--viewport-fit stretch|contain] [--no-control]");
            System.err.println(ex.getMessage()); return;
        }
        java.awt.GraphicsDevice[] displays=java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if (listDisplays) {
            for (int i=0;i<displays.length;i++)
                System.out.println("Display "+i+": "+displays[i].getIDstring()+" "+displays[i].getDefaultConfiguration().getBounds());
            return;
        }
        if (display >= displays.length) {
            System.err.println("Display "+display+" unavailable; use --list-displays. No game started."); return;
        }
        Board[] holder=new Board[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> holder[0]=new Board());
        Board board=holder[0];
        final boolean usePresentation=presentation;
        final int chosenDisplay=display, initialWidth=windowWidth, initialHeight=windowHeight;
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            if (usePresentation) PresentationLayout.install(board.controlWindow(),initialWidth,initialHeight);
            if (chosenDisplay >= 0) {
                java.awt.Rectangle screen=displays[chosenDisplay].getDefaultConfiguration().getBounds();
                board.controlWindow().setLocation(screen.x+Math.max(0,(screen.width-board.controlWindow().getWidth())/2),
                                                 screen.y+Math.max(0,(screen.height-board.controlWindow().getHeight())/2));
            }
        });
        if (arduinoPort != null) {
            ArduinoFeedback feedback=new ArduinoFeedback(arduinoPort);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                feedback.close();
                feedback.awaitClosed();
            }, "guess-who-arduino-shutdown"));
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                board.setCharacterFeedback(feedback::actionCompleted);
                board.controlWindow().addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override public void windowClosing(java.awt.event.WindowEvent event) { feedback.close(); }
                    @Override public void windowClosed(java.awt.event.WindowEvent event) { feedback.close(); }
                });
            });
        } else {
            System.out.println("ARDUINO disabled: use --arduino-port COM5 to enable optional feedback.");
        }
        if (enabled) {
            SwingSelectionController controller=new SwingSelectionController(board.controlWindow(),width,height,contain,board::controlName);
            try {
                ControlServer server=new ControlServer(port,width,height,request ->
                    "INFO".equals(request.get("type")) ? controller.info()
                    : controller.select((Double)request.get("x"),(Double)request.get("y")));
                javax.swing.SwingUtilities.invokeAndWait(() -> board.controlWindow().addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override public void windowClosing(java.awt.event.WindowEvent event) { server.close(); }
                }));
                Runtime.getRuntime().addShutdownHook(new Thread(server::close));
                server.start();
            } catch(java.io.IOException ex) {
                System.err.println("CONTROL disabled: "+ex.getMessage()+". Mouse game remains available; choose another --control-port.");
            }
        }
    }
}
