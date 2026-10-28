package groupDGuessWho;

import java.awt.geom.Point2D;

/** Logical viewport pixels -> current Swing content coordinates, excluding decorations. */
final class ViewportMapping {
    final int viewportWidth, viewportHeight, contentWidth, contentHeight;
    final double left, top, width, height;
    ViewportMapping(int vw, int vh, int cw, int ch, boolean contain) {
        if (vw < 2 || vh < 2 || cw < 2 || ch < 2) throw new IllegalArgumentException("Viewport/content too small");
        viewportWidth=vw; viewportHeight=vh; contentWidth=cw; contentHeight=ch;
        double scale = Math.min((double)vw/cw, (double)vh/ch);
        width = contain ? cw*scale : vw;
        height = contain ? ch*scale : vh;
        left=(vw-width)/2; top=(vh-height)/2;
    }
    Point2D.Double toContent(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || x < 0 || y < 0
                || x > viewportWidth-1 || y > viewportHeight-1)
            throw new IllegalArgumentException("Coordinates outside configured viewport");
        if (x < left || y < top || x > left+width-1 || y > top+height-1) return null;
        return new Point2D.Double((x-left)*(contentWidth-1)/(width-1),
                                 (y-top)*(contentHeight-1)/(height-1));
    }
    Point2D.Double toViewport(double x, double y) {
        return new Point2D.Double(left+x*(width-1)/(contentWidth-1), top+y*(height-1)/(contentHeight-1));
    }
}
