package vectoria.modid.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import vectoria.modid.plot.Camera3D;
import vectoria.modid.plot.Colors;
import vectoria.modid.plot.Format;
import vectoria.modid.plot.PlotFunction;
import vectoria.modid.plot.SurfaceGrid;

public final class Plot3DScreen extends PlotScreen {
    private static final int CELLS = 32;
    private static final int[][] RAMPS = {
            {0xFF3B82F6, 0xFFF87171},
            {0xFF10B981, 0xFFFBBF24}
    };
    private static final String[] HINTS = {"z = a*sin(x)*cos(y)", "z = (x^2 + y^2) / b"};
    private static final int LABEL_COLOR = 0xFF8A8C9A;
    private static final double DEFAULT_RANGE = 5.0;

    private final Camera3D camera = new Camera3D();
    private final SurfaceGrid[] grids = new SurfaceGrid[RAMPS.length];
    private double range = DEFAULT_RANGE;

    public Plot3DScreen(Screen parent) {
        super(Text.literal("3D Plotter"), parent, PlotFunction.Mode.SPACE, RAMPS.length);
    }

    @Override
    protected String hint(int index) {
        return HINTS[index];
    }

    @Override
    protected int rowColor(int index) {
        return RAMPS[index][1];
    }

    @Override
    protected void resetView() {
        camera.reset();
        range = DEFAULT_RANGE;
        dirty = true;
    }

    @Override
    protected void onPlotDrag(double deltaX, double deltaY) {
        camera.rotate(deltaX * 0.01, deltaY * 0.01);
    }

    @Override
    protected void onPlotScroll(double mouseX, double mouseY, double amount) {
        range = Math.max(0.5, Math.min(500.0, range * Math.pow(0.9, amount)));
        dirty = true;
    }

    @Override
    protected void renderPlot(DrawContext context, int mouseX, int mouseY) {
        if (dirty) {
            rebuild();
            dirty = false;
        }

        int left = plotLeft();
        int right = width;
        int bottom = height;
        double centerX = left + plotWidth() / 2.0;
        double centerY = height / 2.0;
        double scale = Math.min(plotWidth(), height) / (range * 3.6);

        drawFrame(context, left, right, bottom, centerX, centerY, scale);

        for (int k = 0; k < grids.length; k++) {
            if (grids[k] != null) drawSurface(context, grids[k], k, left, right, bottom, centerX, centerY, scale);
        }

        context.drawText(textRenderer, "drag: rotate   scroll: zoom   range +/-" + Format.compact(range),
                left + 6, 6, LABEL_COLOR, false);
    }

    private void rebuild() {
        for (int i = 0; i < grids.length; i++) {
            PlotFunction function = functions[i];
            if (function == null) {
                grids[i] = null;
                continue;
            }
            function.bind(params());
            grids[i] = SurfaceGrid.sample(function, range, CELLS);
        }
    }

    private void drawFrame(DrawContext context, int left, int right, int bottom,
                           double centerX, double centerY, double scale) {
        double[] a = new double[3];
        double[] b = new double[3];
        double[][] corners = {{-range, -range}, {range, -range}, {range, range}, {-range, range}};

        for (int i = 0; i < 4; i++) {
            double[] from = corners[i];
            double[] to = corners[(i + 1) % 4];
            camera.project(from[0], from[1], -range, scale, centerX, centerY, a);
            camera.project(to[0], to[1], -range, scale, centerX, centerY, b);
            LineRenderer.line(context, a[0], a[1], b[0], b[1], left, 0, right, bottom, 0xFF33343F, 1);
        }

        drawAxis(context, left, right, bottom, centerX, centerY, scale, 0, "x", 0xFFE06C75);
        drawAxis(context, left, right, bottom, centerX, centerY, scale, 1, "y", 0xFF98C379);
        drawAxis(context, left, right, bottom, centerX, centerY, scale, 2, "z", 0xFF61AFEF);
    }

    private void drawAxis(DrawContext context, int left, int right, int bottom,
                          double centerX, double centerY, double scale, int axis, String label, int color) {
        double[] a = new double[3];
        double[] b = new double[3];
        double[] tip = new double[3];

        double[] start = new double[3];
        double[] end = new double[3];
        start[axis] = -range;
        end[axis] = range;

        camera.project(start[0], start[1], start[2], scale, centerX, centerY, a);
        camera.project(end[0], end[1], end[2], scale, centerX, centerY, b);
        LineRenderer.line(context, a[0], a[1], b[0], b[1], left, 0, right, bottom, color, 1);

        double[] labelPoint = new double[3];
        labelPoint[axis] = range * 1.08;
        camera.project(labelPoint[0], labelPoint[1], labelPoint[2], scale, centerX, centerY, tip);
        int lx = (int) tip[0];
        int ly = (int) tip[1];
        if (lx >= left && lx < right - 8 && ly >= 0 && ly < bottom - 10) {
            context.drawText(textRenderer, label, lx, ly - 4, color, false);
        }
    }

    private void drawSurface(DrawContext context, SurfaceGrid grid, int rampIndex, int left, int right, int bottom,
                             double centerX, double centerY, double scale) {
        int n = grid.cells;
        int stride = n + 1;
        double[] sx = new double[stride * stride];
        double[] sy = new double[stride * stride];
        double[] sz = new double[stride * stride];
        boolean[] visible = new boolean[stride * stride];
        double[] out = new double[3];

        for (int j = 0; j <= n; j++) {
            for (int i = 0; i <= n; i++) {
                double z = grid.z(i, j);
                int index = j * stride + i;
                if (Double.isNaN(z) || Math.abs(z) > range) continue;

                camera.project(grid.coordinate(i), grid.coordinate(j), z, scale, centerX, centerY, out);
                sx[index] = out[0];
                sy[index] = out[1];
                sz[index] = z;
                visible[index] = true;
            }
        }

        int low = RAMPS[rampIndex][0];
        int high = RAMPS[rampIndex][1];

        for (int j = 0; j <= n; j++) {
            for (int i = 0; i <= n; i++) {
                int index = j * stride + i;
                if (!visible[index]) continue;
                if (i < n && visible[index + 1]) {
                    segment(context, sx, sy, sz, index, index + 1, low, high, left, right, bottom);
                }
                if (j < n && visible[index + stride]) {
                    segment(context, sx, sy, sz, index, index + stride, low, high, left, right, bottom);
                }
            }
        }
    }

    private void segment(DrawContext context, double[] sx, double[] sy, double[] sz, int a, int b,
                         int low, int high, int left, int right, int bottom) {
        double t = ((sz[a] + sz[b]) / 2.0 + range) / (2.0 * range);
        LineRenderer.line(context, sx[a], sy[a], sx[b], sy[b], left, 0, right, bottom, Colors.lerp(low, high, t), 2);
    }
}