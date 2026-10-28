package groupDGuessWho;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import javax.swing.*;

/** Real Board + Swing/TCP, fake Arduino. Never opens a physical serial port. */
public final class ArduinoFeedbackTest {
    private static int passed;
    private static boolean presentation;
    public static void main(String[] args) throws Exception {
        try {
            presentation = java.util.Arrays.asList(args).contains("--presentation");
            workerTests();
            boardTests();
            System.out.println("PASS: " + passed + " Phase 5B-2 checks");
            System.exit(0); // Original sound clips may retain mixer threads in GUI fixtures.
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        passed++;
        System.out.println("PASS " + name);
    }
    private static void await(BooleanSupplier condition) throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime()>end) throw new AssertionError("Timed out in test");
            Thread.sleep(5);
        }
    }
    private static final class Fake implements ArduinoFeedback.Session {
        final AtomicInteger blinks=new AtomicInteger(), closes=new AtomicInteger();
        volatile boolean fail;
        volatile CountDownLatch blocked;
        public String blink() throws IOException {
            if (SwingUtilities.isEventDispatchThread()) throw new AssertionError("Serial on EDT");
            blinks.incrementAndGet();
            if (fail) throw new IOException("simulated disconnect");
            CountDownLatch waitForRelease=blocked;
            if (waitForRelease!=null) {
                try { waitForRelease.await(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
            }
            return "ACK BLINK";
        }
        public void close() { closes.incrementAndGet(); }
    }
    private static void workerTests() throws Exception {
        Fake fake=new Fake();
        AtomicInteger opens=new AtomicInteger();
        ArduinoFeedback feedback=new ArduinoFeedback("FAKE", port -> {
            if (SwingUtilities.isEventDispatchThread()) throw new AssertionError("Open on EDT");
            opens.incrementAndGet(); return fake;
        });
        try {
            await(feedback::isReady);
            SwingUtilities.invokeAndWait(() -> feedback.actionCompleted("one"));
            await(() -> fake.blinks.get()==1);
            check(opens.get()==1, "one action produces one BLINK using one persistent connection");
            fake.blocked=new CountDownLatch(1);
            SwingUtilities.invokeAndWait(() -> feedback.actionCompleted("slow"));
            await(() -> fake.blinks.get()==2);
            FutureTask<Boolean> responsive=new FutureTask<>(() -> true);
            SwingUtilities.invokeLater(responsive);
            check(responsive.get(1,TimeUnit.SECONDS), "EDT responds while serial is blocked");
            fake.blocked.countDown(); fake.blocked=null;
            fake.fail=true;
            feedback.actionCompleted("error");
            await(() -> !feedback.isReady());
            feedback.actionCompleted("after error");
            check(fake.blinks.get()==3 && opens.get()==1, "serial failure disables feedback without retry or replay");
        } finally { feedback.close(); feedback.awaitClosed(); }
        check(fake.closes.get()==1, "worker closes connection once");

        CountDownLatch attempted=new CountDownLatch(1);
        ArduinoFeedback absent=new ArduinoFeedback("MISSING", port -> {
            attempted.countDown(); throw new IOException("missing port");
        });
        attempted.await(2,TimeUnit.SECONDS);
        absent.awaitClosed();
        SwingUtilities.invokeAndWait(() -> absent.actionCompleted("offline game action"));
        check(!absent.isReady(), "unavailable Arduino does not propagate error to EDT");
        absent.close();

        Fake pending=new Fake();
        CountDownLatch opening=new CountDownLatch(1), release=new CountDownLatch(1);
        ArduinoFeedback startup=new ArduinoFeedback("FAKE", port -> {
            opening.countDown();
            try { release.await(); } catch (InterruptedException e) { throw new IOException(e); }
            return pending;
        });
        opening.await(2,TimeUnit.SECONDS);
        startup.actionCompleted("during reset");
        release.countDown(); await(startup::isReady);
        startup.close(); startup.awaitClosed();
        check(pending.blinks.get()==0 && pending.closes.get()==1, "startup events not replayed and shutdown releases port");
    }
    private static Object get(Board board,String name) throws Exception {
        var field=Board.class.getDeclaredField(name); field.setAccessible(true); return field.get(board);
    }
    private static void set(Board board,String name,Object value) throws Exception {
        var field=Board.class.getDeclaredField(name); field.setAccessible(true); field.set(board,value);
    }
    private static void boardTests() throws Exception {
        Board[] holder=new Board[1];
        Fake fake=new Fake();
        ArduinoFeedback feedback=new ArduinoFeedback("FAKE", port -> fake);
        await(feedback::isReady);
        AtomicInteger events=new AtomicInteger();
        try {
            SwingUtilities.invokeAndWait(() -> {
                Board b=new Board(); holder[0]=b;
                if (presentation) PresentationLayout.install(b.controlWindow(),1000,625);
                b.setCharacterFeedback(action -> { events.incrementAndGet(); feedback.actionCompleted(action); });
                try {
                    JButton start=(JButton)get(b,"startGame");
                    for (int i=0;i<5;i++) start.doClick(0);
                    b.controlWindow().validate();
                    if (presentation) {
                        JFrame frame=b.controlWindow();
                        SwingSelectionController mapping=new SwingSelectionController(frame,1280,720,false,b::controlName);
                        for (int[] size : new int[][]{{1200,675},{1024,768},{1000,625}}) {
                            var in=frame.getInsets();
                            frame.setSize(size[0]+in.left+in.right,size[1]+in.top+in.bottom);
                            frame.validate();
                            @SuppressWarnings("unchecked")
                            var controls=(java.util.List<Map<String,Object>>)mapping.info().get("controls");
                            check(controls.stream().filter(c -> ((String)c.get("control")).startsWith("character[")).count()==24,
                                  "all 24 real card controls reachable at "+size[0]+"x"+size[1]);
                        }
                        var content=frame.getContentPane();
                        var image=new java.awt.image.BufferedImage(content.getWidth(),content.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                        var graphics=image.createGraphics();
                        try { content.printAll(graphics); } finally { graphics.dispose(); }
                        var imageFile=new File("tmp/phase7a/presentation-game.png");
                        imageFile.getParentFile().mkdirs();
                        javax.imageio.ImageIO.write(image,"png",imageFile);
                    }
                    check(events.get()==0, "menu actions produce no character feedback");
                    JButton card=((JButton[])get(b,"characterDisplay"))[0];
                    long now=System.currentTimeMillis();
                    card.dispatchEvent(new MouseEvent(card,MouseEvent.MOUSE_PRESSED,now,0,10,10,1,false,MouseEvent.BUTTON1));
                    card.dispatchEvent(new MouseEvent(card,MouseEvent.MOUSE_RELEASED,now+1,0,10,10,1,false,MouseEvent.BUTTON1));
                    check(events.get()==1 && ((int[])get(b,"characterStatus"))[0]==1,
                            "mouse events change real card and emit one completed event");
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            await(() -> fake.blinks.get()==1);
            Board board=holder[0];
            SwingSelectionController selection=new SwingSelectionController(board.controlWindow(),1280,720,false,board::controlName);
            Point2D.Double[] point=new Point2D.Double[1];
            SwingUtilities.invokeAndWait(() -> {
                try {
                    JButton card=((JButton[])get(board,"characterDisplay"))[0];
                    var p=SwingUtilities.convertPoint(card,card.getWidth()/2,card.getHeight()/2,board.controlWindow().getContentPane());
                    var content=board.controlWindow().getContentPane();
                    point[0]=new ViewportMapping(1280,720,content.getWidth(),content.getHeight(),false).toViewport(p.x,p.y);
                } catch(Exception e) { throw new RuntimeException(e); }
            });
            try (ControlServer server=new ControlServer(0,1280,720,r -> "INFO".equals(r.get("type"))
                    ? selection.info() : selection.select((Double)r.get("x"),(Double)r.get("y")))) {
                server.start();
                String select="{\"type\":\"SELECT\",\"id\":\"feedback-test\",\"x\":"+point[0].x+",\"y\":"+point[0].y+"}";
                check(Boolean.TRUE.equals(request(server.port(),select).get("executed")), "TCP webcam route executes actual Board listener");
                await(() -> fake.blinks.get()==2);
                check(events.get()==2, "mouse and webcam use the same single feedback callback");
                check("duplicate".equals(request(server.port(),select).get("status")) && events.get()==2,
                        "duplicate TCP ID produces no second feedback event");
                check("rejected".equals(request(server.port(),"{\"type\":\"SELECT\",\"x\":-1,\"y\":0}").get("status")),
                        "invalid TCP coordinate rejected");
                check("no_control".equals(request(server.port(),"{\"type\":\"SELECT\",\"x\":0,\"y\":0}").get("status"))
                        && events.get()==2, "background click produces no feedback");
                request(server.port(),"{\"type\":\"INFO\"}");
                check(events.get()==2, "INFO does not trigger feedback");
            }
            SwingUtilities.invokeAndWait(() -> {
                try {
                    JButton card=((JButton[])get(board,"characterDisplay"))[0];
                    card.setEnabled(false); card.doClick(0); card.setEnabled(true);
                    check(events.get()==2, "disabled card produces no feedback");
                    set(board,"retrace",true); set(board,"continueSmallStatus",1);
                    card.doClick(0);
                    check(events.get()==2, "ignored retrace selection produces no feedback");
                    set(board,"retrace",false); set(board,"panelOnScreen",0);
                    card.doClick(0);
                    check(events.get()==3, "valid guess emits feedback after game handler completes");
                    card.doClick(0);
                    check(events.get()==3, "already-targeted card does not emit duplicate feedback");
                    // A callback failure must not break existing game action execution.
                    board.setCharacterFeedback(action -> { throw new IllegalStateException("fake feedback bug"); });
                    set(board,"panelOnScreen",1);
                    card.doClick(0);
                    check(((int[])get(board,"characterStatus"))[0]==1, "feedback exception does not prevent completed game state change");
                } catch(Exception e) { throw new RuntimeException(e); }
            });
            await(() -> fake.blinks.get()==3);
        } finally {
            feedback.close(); feedback.awaitClosed();
            SwingUtilities.invokeAndWait(() -> {
                if (holder[0]!=null) { holder[0].play.stopMusic(); holder[0].controlWindow().dispose(); }
            });
        }
    }
    private static Map<String,Object> request(int port,String json) throws IOException {
        try (Socket socket=new Socket("127.0.0.1",port)) {
            socket.setSoTimeout(3000);
            socket.getOutputStream().write((json+"\n").getBytes(StandardCharsets.UTF_8));
            String response=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8)).readLine();
            // Production request parser intentionally accepts only flat scalar objects.
            // Test response inspection needs only these simple status/executed fields.
            return Map.of("status", response.contains("\"status\":\"duplicate\"") ? "duplicate"
                    : response.contains("\"status\":\"rejected\"") ? "rejected"
                    : response.contains("\"status\":\"no_control\"") ? "no_control" : "executed",
                    "executed", response.contains("\"executed\":true"));
        }
    }
}
