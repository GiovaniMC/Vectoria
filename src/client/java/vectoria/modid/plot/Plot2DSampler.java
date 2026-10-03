package vectoria.modid.plot;

import java.util.ArrayList;
import java.util.List;

public final class Plot2DSampler {
    private static final double SAMPLE_STEP = 2.0;
    private static final int CELL = 8;

    private Plot2DSampler() {
    }

    public static List<double[]> explicit(PlotFunction function, ViewPort2D view) {
        List<double[]> segments = new ArrayList<>();
        int samples = (int) Math.ceil(view.width / SAMPLE_STEP);

        boolean hasPrevious = false;
        double previousX = 0.0;
        double previousY = 0.0;

        for (int i = 0; i <= samples; i++) {
            double px = view.left + Math.min(i * SAMPLE_STEP, view.width);
            double value = function.value(view.worldX(px), 0.0);

            if (Double.isNaN(value)) {
                hasPrevious = false;
                continue;
            }

            double py = view.screenY(value);
            if (hasPrevious && Math.abs(py - previousY) <= view.height * 3.0) {
                segments.add(new double[]{previousX, previousY, px, py});
            }

            previousX = px;
            previousY = py;
            hasPrevious = true;
        }

        return segments;
    }

    public static List<double[]> implicit(PlotFunction function, ViewPort2D view) {
        List<double[]> segments = new ArrayList<>();
        int cols = (int) Math.ceil(view.width / (double) CELL);
        int rows = (int) Math.ceil(view.height / (double) CELL);
        int stride = cols + 1;

        double[] values = new double[stride * (rows + 1)];
        for (int j = 0; j <= rows; j++) {
            for (int i = 0; i <= cols; i++) {
                values[j * stride + i] = function.value(
                        view.worldX(view.left + i * CELL),
                        view.worldY(view.top + j * CELL)
                );
            }
        }

        for (int j = 0; j < rows; j++) {
            for (int i = 0; i < cols; i++) {
                double v0 = values[j * stride + i];
                double v1 = values[j * stride + i + 1];
                double v2 = values[(j + 1) * stride + i + 1];
                double v3 = values[(j + 1) * stride + i];

                if (Double.isNaN(v0) || Double.isNaN(v1) || Double.isNaN(v2) || Double.isNaN(v3)) continue;

                int index = (v0 > 0 ? 8 : 0) | (v1 > 0 ? 4 : 0) | (v2 > 0 ? 2 : 0) | (v3 > 0 ? 1 : 0);
                if (index == 0 || index == 15) continue;

                double x0 = view.left + i * CELL;
                double y0 = view.top + j * CELL;

                double[] top = (v0 > 0) != (v1 > 0) ? new double[]{x0 + CELL * (v0 / (v0 - v1)), y0} : null;
                double[] right = (v1 > 0) != (v2 > 0) ? new double[]{x0 + CELL, y0 + CELL * (v1 / (v1 - v2))} : null;
                double[] bottom = (v3 > 0) != (v2 > 0) ? new double[]{x0 + CELL * (v3 / (v3 - v2)), y0 + CELL} : null;
                double[] left = (v0 > 0) != (v3 > 0) ? new double[]{x0, y0 + CELL * (v0 / (v0 - v3))} : null;

                double center = (v0 + v1 + v2 + v3) / 4.0;

                switch (index) {
                    case 1, 14 -> add(segments, left, bottom);
                    case 2, 13 -> add(segments, bottom, right);
                    case 3, 12 -> add(segments, left, right);
                    case 4, 11 -> add(segments, top, right);
                    case 6, 9 -> add(segments, top, bottom);
                    case 7, 8 -> add(segments, left, top);
                    case 5 -> {
                        if (center > 0) {
                            add(segments, left, top);
                            add(segments, bottom, right);
                        } else {
                            add(segments, top, right);
                            add(segments, left, bottom);
                        }
                    }
                    case 10 -> {
                        if (center > 0) {
                            add(segments, top, right);
                            add(segments, left, bottom);
                        } else {
                            add(segments, left, top);
                            add(segments, bottom, right);
                        }
                    }
                    default -> { }
                }
            }
        }

        return segments;
    }

    private static void add(List<double[]> segments, double[] a, double[] b) {
        segments.add(new double[]{a[0], a[1], b[0], b[1]});
    }
}