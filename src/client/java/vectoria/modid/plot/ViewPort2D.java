package vectoria.modid.plot;

public final class ViewPort2D {
    private static final double DEFAULT_SCALE = 50.0;

    public double centerX = 0.0;
    public double centerY = 0.0;
    public double scale = DEFAULT_SCALE;
    public int left = 0;
    public int top = 0;
    public int width = 1;
    public int height = 1;

    private double lastCenterX = Double.NaN;
    private double lastCenterY;
    private double lastScale;
    private int lastLeft;
    private int lastTop;
    private int lastWidth;
    private int lastHeight;

    public double centerPixelX() {
        return left + width / 2.0;
    }

    public double centerPixelY() {
        return top + height / 2.0;
    }

    public double worldX(double pixelX) {
        return centerX + (pixelX - centerPixelX()) / scale;
    }

    public double worldY(double pixelY) {
        return centerY - (pixelY - centerPixelY()) / scale;
    }

    public double screenX(double worldX) {
        return centerPixelX() + (worldX - centerX) * scale;
    }

    public double screenY(double worldY) {
        return centerPixelY() - (worldY - centerY) * scale;
    }

    public void pan(double deltaPixelsX, double deltaPixelsY) {
        centerX -= deltaPixelsX / scale;
        centerY += deltaPixelsY / scale;
    }

    public void zoomAt(double pixelX, double pixelY, double factor) {
        double wx = worldX(pixelX);
        double wy = worldY(pixelY);
        scale = Math.max(0.5, Math.min(100000.0, scale * factor));
        centerX = wx - (pixelX - centerPixelX()) / scale;
        centerY = wy + (pixelY - centerPixelY()) / scale;
    }

    public void reset() {
        centerX = 0.0;
        centerY = 0.0;
        scale = DEFAULT_SCALE;
    }

    public double gridStep() {
        return niceStep(80.0 / scale);
    }

    public static double niceStep(double raw) {
        double magnitude = Math.pow(10.0, Math.floor(Math.log10(raw)));
        double fraction = raw / magnitude;
        double nice;
        if (fraction <= 1.0) nice = 1.0;
        else if (fraction <= 2.0) nice = 2.0;
        else if (fraction <= 5.0) nice = 5.0;
        else nice = 10.0;
        return nice * magnitude;
    }

    public boolean consumeChange() {
        boolean changed = Double.isNaN(lastCenterX)
                || lastCenterX != centerX
                || lastCenterY != centerY
                || lastScale != scale
                || lastLeft != left
                || lastTop != top
                || lastWidth != width
                || lastHeight != height;

        lastCenterX = centerX;
        lastCenterY = centerY;
        lastScale = scale;
        lastLeft = left;
        lastTop = top;
        lastWidth = width;
        lastHeight = height;
        return changed;
    }
}