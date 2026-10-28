package groupDGuessWho;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import javax.swing.*;

/** All methods run on the EDT. Uses actual hierarchy, bounds, visibility and z-order. */
final class SwingSelectionController {
    private final JFrame frame;
    private final int vw, vh;
    private final boolean contain;
    private final Function<JButton,String> name;
    SwingSelectionController(JFrame frame, int vw, int vh, boolean contain, Function<JButton,String> name) {
        this.frame=frame; this.vw=vw; this.vh=vh; this.contain=contain; this.name=name;
    }
    private void check() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("EDT required");
        if (!frame.isShowing()) throw new IllegalStateException("Game window is not showing");
    }
    private ViewportMapping mapping() {
        Container content=frame.getContentPane();
        return new ViewportMapping(vw,vh,content.getWidth(),content.getHeight(),contain);
    }
    private JButton buttonAt(int x, int y) {
        Component c=SwingUtilities.getDeepestComponentAt(frame.getContentPane(),x,y);
        while (c != null && c != frame.getContentPane()) {
            if (c instanceof JButton b) return b.isShowing() && b.isEnabled() ? b : null;
            c=c.getParent();
        }
        return null;
    }
    Map<String,Object> select(double x,double y) {
        check();
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("received_x",x); result.put("received_y",y); result.put("executed",false);
        Point2D.Double p=mapping().toContent(x,y);
        if (p == null) { result.put("status","unused_margin"); return result; }
        int cx=(int)Math.round(p.x), cy=(int)Math.round(p.y);
        result.put("java_x",cx); result.put("java_y",cy);
        JButton button=buttonAt(cx,cy);
        if (button == null) { result.put("status","no_control"); return result; }
        result.put("control",name.apply(button));
        try {
            // Runs every original listener, including the separate click sound.
            button.doClick(0);
            result.put("status","executed"); result.put("executed",true);
        } catch (RuntimeException ex) {
            result.put("status","action_error"); result.put("error",ex.toString());
        }
        return result;
    }
    Map<String,Object> info() {
        check();
        ViewportMapping m=mapping();
        List<Map<String,Object>> controls=new ArrayList<>();
        collect(frame.getContentPane(),m,controls);
        Map<String,Object> info=new LinkedHashMap<>();
        info.put("status","info"); info.put("executed",false);
        info.put("viewport_width",vw); info.put("viewport_height",vh);
        info.put("content_width",m.contentWidth); info.put("content_height",m.contentHeight);
        info.put("fit",contain ? "contain" : "stretch");
        info.put("viewport_left",m.left); info.put("viewport_top",m.top);
        info.put("controls",controls); return info;
    }
    private void collect(Container parent,ViewportMapping m,List<Map<String,Object>> out) {
        for (Component c:parent.getComponents()) {
            if (!c.isShowing()) continue;
            if (c instanceof JButton b && b.isEnabled()) {
                Rectangle bounds=SwingUtilities.convertRectangle(b.getParent(),b.getBounds(),frame.getContentPane());
                Rectangle visible=bounds.intersection(new Rectangle(0,0,m.contentWidth,m.contentHeight));
                int x=visible.x+visible.width/2, y=visible.y+visible.height/2;
                if (!visible.isEmpty() && buttonAt(x,y)==b) {
                    Point2D.Double p=m.toViewport(x,y);
                    Map<String,Object> item=new LinkedHashMap<>();
                    item.put("control",name.apply(b)); item.put("x",p.x); item.put("y",p.y);
                    item.put("java_x",x); item.put("java_y",y); out.add(item);
                }
            }
            if (c instanceof Container child) collect(child,m,out);
        }
    }
}
