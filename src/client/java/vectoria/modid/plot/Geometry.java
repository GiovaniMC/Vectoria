package vectoria.modid.plot;

public final class Geometry {

    private Geometry() {
    }

    public static boolean clip(double[] segment, double xMin, double yMin, double xMax, double yMax) {
        double x0 = segment[0];
        double y0 = segment[1];
        double dx = segment[2] - x0;
        double dy = segment[3] - y0;

        double[] p = {-dx, dx, -dy, dy};
        double[] q = {x0 - xMin, xMax - x0, y0 - yMin, yMax - y0};

        double t0 = 0.0;
        double t1 = 1.0;

        for (int i = 0; i < 4; i++) {
            if (p[i] == 0.0) {
                if (q[i] < 0.0) return false;
            } else {
                double r = q[i] / p[i];
                if (p[i] < 0.0) {
                    if (r > t1) return false;
                    if (r > t0) t0 = r;
                } else {
                    if (r < t0) return false;
                    if (r < t1) t1 = r;
                }
            }
        }

        segment[0] = x0 + t0 * dx;
        segment[1] = y0 + t0 * dy;
        segment[2] = x0 + t1 * dx;
        segment[3] = y0 + t1 * dy;
        return true;
    }
}