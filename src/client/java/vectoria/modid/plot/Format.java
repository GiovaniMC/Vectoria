package vectoria.modid.plot;

import java.util.Locale;

public final class Format {

    private Format() {
    }

    public static String axis(double value, double step) {
        if (Math.abs(value) < step * 1e-6) return "0";

        double abs = Math.abs(value);
        if (abs >= 1e7 || abs < 1e-4) return String.format(Locale.ROOT, "%.2e", value);

        int decimals = (int) Math.max(0, Math.min(8, Math.ceil(-Math.log10(step))));
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    public static String compact(double value) {
        String s = String.format(Locale.ROOT, "%.2f", value);
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s.equals("-0") ? "0" : s;
    }
}