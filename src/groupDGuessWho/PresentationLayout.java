package groupDGuessWho;

import java.awt.*;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.swing.*;

/** Optional scaling of the original 800x500 layout; retains real Swing controls. */
final class PresentationLayout implements LayoutManager {
    private final Map<Component, Rectangle> bounds = new IdentityHashMap<>();
    private final Map<Component, Font> fonts = new IdentityHashMap<>();
    private double sx = 1, sy = 1;
    private boolean updating;

    static void install(JFrame frame, int width, int height) {
        Container content = frame.getContentPane();
        PresentationLayout layout = new PresentationLayout();
        for (Component component : content.getComponents())
            layout.bounds.put(component, component.getBounds());
        layout.observe(content);
        content.setLayout(layout);
        content.setPreferredSize(new Dimension(width, height));
        frame.setResizable(true);
        frame.pack();
        Insets insets = frame.getInsets();
        frame.setMinimumSize(new Dimension(800 + insets.left + insets.right,
                                           500 + insets.top + insets.bottom));
        frame.validate();
    }

    private void observe(Component c) {
        if (c.getFont() != null) fonts.put(c, c.getFont());
        c.addPropertyChangeListener("font", e -> {
            if (!updating && e.getNewValue() instanceof Font font) {
                fonts.put(c, font);
                scaleFont(c, font);
            }
        });
        c.addPropertyChangeListener("icon", e -> {
            if (!(e.getNewValue() instanceof ScaledIcon)) wrapIcon(c);
        });
        wrapIcon(c);
        if (c instanceof Container parent)
            for (Component child : parent.getComponents()) observe(child);
    }

    private void scaleFont(Component c, Font original) {
        updating = true;
        try { c.setFont(original.deriveFont((float)(original.getSize2D() * Math.min(sx, sy)))); }
        finally { updating = false; }
    }

    private void wrapIcon(Component c) {
        Icon icon = c instanceof AbstractButton b ? b.getIcon() : c instanceof JLabel l ? l.getIcon() : null;
        if (icon == null || icon instanceof ScaledIcon) return;
        Icon scaled = new ScaledIcon(icon);
        if (c instanceof AbstractButton b) b.setIcon(scaled);
        else ((JLabel)c).setIcon(scaled);
    }

    private final class ScaledIcon implements Icon {
        private final Icon source;
        ScaledIcon(Icon source) { this.source = source; }
        public int getIconWidth() { return (int)Math.round(source.getIconWidth() * sx); }
        public int getIconHeight() { return (int)Math.round(source.getIconHeight() * sy); }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D copy = (Graphics2D)g.create();
            try {
                copy.translate(x, y);
                copy.scale(sx, sy);
                copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                source.paintIcon(c, copy, 0, 0);
            } finally { copy.dispose(); }
        }
        // Existing card feedback compares image filenames before/after an action.
        @Override public String toString() { return source.toString(); }
    }

    public void layoutContainer(Container parent) {
        sx = parent.getWidth() / 800.0;
        sy = parent.getHeight() / 500.0;
        for (Map.Entry<Component, Rectangle> entry : bounds.entrySet()) {
            Rectangle b = entry.getValue();
            int x = (int)Math.round(b.x*sx), y = (int)Math.round(b.y*sy);
            entry.getKey().setBounds(x, y, (int)Math.round((b.x+b.width)*sx)-x,
                                            (int)Math.round((b.y+b.height)*sy)-y);
        }
        fonts.forEach(this::scaleFont);
        parent.repaint();
    }
    public Dimension preferredLayoutSize(Container parent) { return new Dimension(800, 500); }
    public Dimension minimumLayoutSize(Container parent) { return new Dimension(800, 500); }
    public void addLayoutComponent(String name, Component c) { bounds.putIfAbsent(c, c.getBounds()); }
    public void removeLayoutComponent(Component c) { bounds.remove(c); }
}
