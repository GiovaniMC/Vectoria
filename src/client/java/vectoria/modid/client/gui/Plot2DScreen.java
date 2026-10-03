package vectoria.modid.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import vectoria.modid.plot.Format;
import vectoria.modid.plot.Plot2DSampler;
import vectoria.modid.plot.PlotFunction;
import vectoria.modid.plot.ViewPort2D;

import java.util.ArrayList;
import java.util.List;

public final class Plot2DScreen extends PlotScreen {
    private static final int[] COLORS = {0xFF4FC3F7, 0xFFFF8A65, 0xFF81C784, 0xFFBA68C8};
    private static final String[] HINTS = {"y = a*sin(b*x)", "x^2 + y^2 = 9", "y = m*x + c", "\\dv{x^2}{x}"};
    private static final int GRID_COLOR = 0xFF22232B;
    private static final int AXIS_COLOR = 0xFF5A5C6B;
    private static final int LABEL_COLOR = 0xFF8A8C9A;

    private final ViewPort2D view = new ViewPort2D();
    private final List<List<double[]>> geometry = new ArrayList<>();

    public Plot2DScreen(Screen parent) {
        super(Text.literal("2D Plotter"), parent, PlotFunction.Mode.PLANE, COLORS.length);
    }

    @Override
    protected String hint(int index) {
        return HINTS[index];
    }

    @Override
    protected int rowColor(int index) {
        return COLORS[index];
    }

    @Override
    protected void resetView() {
        view.reset();
    }

    @Override
    protected void onPlotDrag(double deltaX, double deltaY) {
        view.pan(deltaX, deltaY);
    }

    @Override
    protected void onPlotScroll(double mouseX, double mouseY, double amount) {
        view.zoomAt(mouseX, mouseY, Math.pow(1.15, amount));
    }

    @Override
    protected void renderPlot(DrawContext context, int mouseX, int mouseY) {
        view.left = plotLeft();
        view.top = 0;
        view.width = plotWidth();
        view.height = height;

        if (view.consumeChange()) dirty = true;
        if (dirty) {
            rebuild();
            dirty = false;
        }

        drawGrid(context);

        int right = view.left + view.width;
        int bottom = view.top + view.height;
        for (int i = 0; i < geometry.size(); i++) {
            for (double[] s : geometry.get(i)) {
                LineRenderer.line(context, s[0], s[1], s[2], s[3], view.left, view.top, right, bottom, rowColor(i), 2);
            }
        }

        drawFooter(context, mouseX, mouseY);
    }

    private void rebuild() {
        geometry.clear();
        for (PlotFunction function : functions) {
            if (function == null) {
                geometry.add(List.of());
                continue;
            }
            function.bind(params());
            geometry.add(function.kind() == PlotFunction.Kind.EXPLICIT
                    ? Plot2DSampler.explicit(function, view)
                    : Plot2DSampler.implicit(function, view));
        }
    }

    private void drawGrid(DrawContext context) {
        double step = view.gridStep();
        int right = view.left + view.width;
        int bottom = view.top + view.height;

        double minX = view.worldX(view.left);
        double maxX = view.worldX(right);
        double minY = view.worldY(bottom);
        double maxY = view.worldY(view.top);

        int originX = (int) Math.round(view.screenX(0));
        int originY = (int) Math.round(view.screenY(0));
        int labelY = Math.max(view.top + 3, Math.min(bottom - 22, originY + 3));
        int labelX = Math.max(view.left + 3, Math.min(right - 44, originX + 4));

        long firstX = (long) Math.ceil(minX / step);
        long lastX = (long) Math.floor(maxX / step);
        for (long k = firstX; k <= lastX && k - firstX < 400; k++) {
            int sx = (int) Math.round(view.screenX(k * step));
            context.fill(sx, view.top, sx + 1, bottom, k == 0 ? AXIS_COLOR : GRID_COLOR);
            if (k != 0) context.drawText(textRenderer, Format.axis(k * step, step), sx + 3, labelY, LABEL_COLOR, false);
        }

        long firstY = (long) Math.ceil(minY / step);
        long lastY = (long) Math.floor(maxY / step);
        for (long k = firstY; k <= lastY && k - firstY < 400; k++) {
            int sy = (int) Math.round(view.screenY(k * step));
            context.fill(view.left, sy, right, sy + 1, k == 0 ? AXIS_COLOR : GRID_COLOR);
            if (k != 0) context.drawText(textRenderer, Format.axis(k * step, step), labelX, sy - 10, LABEL_COLOR, false);
        }

        if (minX <= 0 && maxX >= 0 && minY <= 0 && maxY >= 0) {
            context.drawText(textRenderer, "0", originX + 3, originY + 3, LABEL_COLOR, false);
        }
    }

    private void drawFooter(DrawContext context, int mouseX, int mouseY) {
        int bottom = view.top + view.height;
        context.drawText(textRenderer, "drag: pan   scroll: zoom", view.left + 6, view.top + 6, LABEL_COLOR, false);

        if (inPlot(mouseX, mouseY)) {
            String readout = "x = " + Format.compact(view.worldX(mouseX)) + ",  y = " + Format.compact(view.worldY(mouseY));
            context.drawText(textRenderer, readout, view.left + 6, bottom - 12, 0xFFD0D0DA, false);
        }
    }
}