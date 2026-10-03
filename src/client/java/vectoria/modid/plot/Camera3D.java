package vectoria.modid.plot;

public final class Camera3D {
    private static final double DEFAULT_YAW = -0.75;
    private static final double DEFAULT_PITCH = 0.55;

    public double yaw = DEFAULT_YAW;
    public double pitch = DEFAULT_PITCH;

    public void rotate(double deltaYaw, double deltaPitch) {
        yaw += deltaYaw;
        pitch = Math.max(-1.5, Math.min(1.5, pitch + deltaPitch));
    }

    public void reset() {
        yaw = DEFAULT_YAW;
        pitch = DEFAULT_PITCH;
    }

    public void project(double x, double y, double z, double scale, double centerX, double centerY, double[] out) {
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);

        double rotatedX = x * cosYaw - y * sinYaw;
        double rotatedY = x * sinYaw + y * cosYaw;

        out[0] = centerX + rotatedX * scale;
        out[1] = centerY - (z * cosPitch + rotatedY * sinPitch) * scale;
        out[2] = rotatedY * cosPitch - z * sinPitch;
    }
}