package groupDGuessWho;

import java.util.*;
import java.awt.geom.Point2D;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;

/** Test-only endpoint using the production TCP server and EDT, no game window. */
public class ControlTestHarness {
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--swing-test")) {
            SwingUtilities.invokeAndWait(() -> {
                javax.swing.JFrame frame=new javax.swing.JFrame("Phase 4B automated UI fixture");
                try {
                    javax.swing.JPanel panel=new javax.swing.JPanel(null);
                    frame.setContentPane(panel);
                    frame.setSize(400,300);
                    javax.swing.JButton button=new javax.swing.JButton("test");
                    button.setBounds(20,20,100,60); panel.add(button);
                    AtomicInteger clicks=new AtomicInteger();
                    button.addActionListener(e -> { check(SwingUtilities.isEventDispatchThread()); clicks.incrementAndGet(); });
                    frame.setVisible(true); frame.validate();
                    SwingSelectionController control=new SwingSelectionController(frame,1280,720,false,b -> b.getText());
                    ViewportMapping m=new ViewportMapping(1280,720,panel.getWidth(),panel.getHeight(),false);
                    Point2D.Double p=m.toViewport(50,50);
                    check(Boolean.TRUE.equals(control.select(p.x,p.y).get("executed")));
                    check(clicks.get()==1);
                    button.setEnabled(false);
                    check(Boolean.FALSE.equals(control.select(p.x,p.y).get("executed")));
                    button.setEnabled(true); button.setVisible(false);
                    check(Boolean.FALSE.equals(control.select(p.x,p.y).get("executed")));
                    button.setVisible(true);
                    javax.swing.JPanel cover=new javax.swing.JPanel(); cover.setBounds(20,20,100,60);
                    panel.add(cover,0); panel.validate();
                    check(Boolean.FALSE.equals(control.select(p.x,p.y).get("executed")));
                    check(clicks.get()==1);
                } finally { frame.dispose(); }
            });
            System.out.println("Swing action, hidden/disabled/covered controls and EDT assertions passed");
            return;
        }
        if (args.length > 0 && args[0].equals("--self-test")) {
            ViewportMapping stretch=new ViewportMapping(1280,720,800,500,false);
            check(stretch.toContent(1279,719).equals(new Point2D.Double(799,499)));
            Point2D.Double center=stretch.toViewport(400,250);
            check(stretch.toContent(center.x,center.y).distance(400,250)<1e-8);
            ViewportMapping contain=new ViewportMapping(1280,720,800,500,true);
            check(contain.toContent(0,360)==null);
            check(contain.toContent(contain.left,0).distance(0,0)<1e-8);
            ViewportMapping resized=new ViewportMapping(1280,720,1000,600,false);
            check(resized.toContent(1279,719).distance(999,599)<1e-8);
            try { stretch.toContent(Double.NaN,0); throw new AssertionError(); } catch(IllegalArgumentException expected) { }
            check(ControlJson.parse("{\"id\":\"a\\u002db\",\"x\":1e2}").get("id").equals("a-b"));
            for(String bad:List.of("{}{}","{\"x\":01}","{\"x\":NaN}","{\"x\":1,\"x\":2}","{\"x\":true}","{\"x\":[]}","{\"id\":\"a\n\"}","{\"x\":1,}")) {
                try { ControlJson.parse(bad); throw new AssertionError(bad); } catch(IllegalArgumentException expected) { }
            }
            System.out.println("Mapping and strict JSON assertions passed"); return;
        }
        AtomicInteger count=new AtomicInteger();
        ControlServer server=new ControlServer(0,1280,720,request -> {
            if (!SwingUtilities.isEventDispatchThread()) throw new AssertionError("Not EDT");
            Map<String,Object> result=new LinkedHashMap<>();
            boolean select="SELECT".equals(request.get("type"));
            result.put("status",select ? "executed" : "info"); result.put("executed",select);
            if (!select) {
                result.put("viewport_width",1280); result.put("viewport_height",720);
                result.put("fit","stretch");
            }
            if(select) count.incrementAndGet();
            result.put("count",count.get()); return result;
        });
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        server.start();
        Thread.currentThread().join();
    }
}
