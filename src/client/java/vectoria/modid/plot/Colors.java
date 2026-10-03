package vectoria.modid.plot;

public final class Colors {

    private Colors() {
    }

    public static int lerp(int from, int to, double t) {
        double clamped = Math.max(0.0, Math.min(1.0, t));

        int r = channel(from >> 16, to >> 16, clamped);
        int g = channel(from >> 8, to >> 8, clamped);
        int b = channel(from, to, clamped);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int channel(int from, int to, double t) {
        int a = from & 255;
        int b = to & 255;
        return (int) Math.round(a + (b - a) * t);
    }
}