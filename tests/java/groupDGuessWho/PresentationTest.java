package groupDGuessWho;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;

/** Real Swing layout/hit testing without camera, projector, serial or game audio. */
public final class PresentationTest {
    private static int checks;
    private static void check(boolean b, String message) {
        if (!b) throw new AssertionError(message);
        checks++;
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame=new JFrame("Phase 7A resize test");
            try {
                JPanel root=new JPanel(null), grid=new JPanel(new GridLayout(1,2));
                frame.setContentPane(root); grid.setBounds(100,100,600,300); root.add(grid);
                JButton button=new JButton("target"), other=new JButton("other"); grid.add(button); grid.add(other);
                ImageIcon original=new ImageIcon(new BufferedImage(75,100,BufferedImage.TYPE_INT_RGB),"card.png");
                button.setIcon(original);
                AtomicInteger clicked=new AtomicInteger(); button.addActionListener(e -> clicked.incrementAndGet());
                frame.setSize(814,537); frame.setVisible(true);
                PresentationLayout.install(frame,1000,625);
                SwingSelectionController control=new SwingSelectionController(frame,1280,720,false,JButton::getText);
                for (Dimension size : List.of(new Dimension(800,500),new Dimension(1280,720),new Dimension(1024,768),new Dimension(1000,625))) {
                    Insets in=frame.getInsets(); frame.setSize(size.width+in.left+in.right,size.height+in.top+in.bottom);
                    frame.validate();
                    check(root.getSize().equals(size),"requested content size");
                    Map<String,Object> info=control.info();
                    var target=((List<Map<String,Object>>)info.get("controls")).stream().filter(c -> c.get("control").equals("target")).findFirst().orElseThrow();
                    int previous=clicked.get();
                    check(Boolean.TRUE.equals(control.select((Double)target.get("x"),(Double)target.get("y")).get("executed")),"mapped selection after resize");
                    check(clicked.get()==previous+1,"one action per mapped selection");
                    long now=System.currentTimeMillis();
                    button.dispatchEvent(new MouseEvent(button,MouseEvent.MOUSE_PRESSED,now,0,10,10,1,false,MouseEvent.BUTTON1));
                    button.dispatchEvent(new MouseEvent(button,MouseEvent.MOUSE_RELEASED,now+1,0,10,10,1,false,MouseEvent.BUTTON1));
                    check(clicked.get()==previous+2,"mouse preserved after resize");
                    check(button.getIcon().getIconWidth()==Math.round(75*size.width/800.0),"artwork scales");
                    check(button.getIcon().toString().equals("card.png"),"feedback filename semantics preserved");
                    check(Boolean.FALSE.equals(control.select(0,0).get("executed")),"background does not select");
                }
                ImageIcon changed=new ImageIcon(new BufferedImage(75,100,BufferedImage.TYPE_INT_RGB),"excluded.png");
                button.setIcon(changed);
                check(button.getIcon().toString().equals("excluded.png"),"new game icon retains identity");
                check(button.getIcon().getIconWidth()==94,"new icon is scaled without resize");
                button.setFont(new Font("Dialog",Font.BOLD,20));
                check(button.getFont().getSize2D()==25,"font changes scaled without feedback loop");
                // Screen origin is deliberately unrelated to content-local mapping.
                frame.setLocation(40,50); frame.validate();
                check(((Integer)control.info().get("viewport_width"))==1280,"moving window keeps logical viewport");
            } finally { frame.dispose(); }
        });
        System.out.println("PASS: "+checks+" Phase 7A presentation checks");
    }
}
