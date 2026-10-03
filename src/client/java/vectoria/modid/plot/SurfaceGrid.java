package vectoria.modid.plot;

public final class SurfaceGrid {
    public final int cells;
    public final double range;
    private final double[] heights;

    private SurfaceGrid(int cells, double range, double[] heights) {
        this.cells = cells;
        this.range = range;
        this.heights = heights;
    }

    public static SurfaceGrid sample(PlotFunction function, double range, int cells) {
        int stride = cells + 1;
        double[] heights = new double[stride * stride];

        for (int j = 0; j <= cells; j++) {
            for (int i = 0; i <= cells; i++) {
                double x = -range + 2.0 * range * i / cells;
                double y = -range + 2.0 * range * j / cells;
                heights[j * stride + i] = function.value(x, y);
            }
        }

        return new SurfaceGrid(cells, range, heights);
    }

    public double coordinate(int index) {
        return -range + 2.0 * range * index / cells;
    }

    public double z(int i, int j) {
        return heights[j * (cells + 1) + i];
    }
}