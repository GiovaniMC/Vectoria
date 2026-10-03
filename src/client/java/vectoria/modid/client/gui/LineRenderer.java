package vectoria.modid.client.gui;

import net.minecraft.client.gui.DrawContext;
import vectoria.modid.plot.Geometry;

public final class LineRenderer {

    private LineRenderer() {
    }

    public static void line(DrawContext context, double x0, double y0, double x1, double y1,
                            int left, int top, int right, int bottom, int color, int thickness) {
        int half = thickness / 2;
        double[] segment = {x0, y0, x1, y1};
        if (!Geometry.clip(segment, left + half, top + half, right - half - 1, bottom - half - 1)) return;

        double dx = segment[2] - segment[0];
        double dy = segment[3] - segment[1];
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / thickness));

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int px = (int) Math.round(segment[0] + dx * t) - half;
            int py = (int) Math.round(segment[1] + dy * t) - half;
            context.fill(px, py, px + thickness, py + thickness, color);
        }
    }
}