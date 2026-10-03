package vectoria.modid.client.gui;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import vectoria.modid.plot.Format;

import java.util.function.DoubleConsumer;

public final class ParamSlider extends SliderWidget {
    private final String name;
    private final double min;
    private final double max;
    private final DoubleConsumer onChange;

    public ParamSlider(int x, int y, int width, int height, String name,
                       double min, double max, double current, DoubleConsumer onChange) {
        super(x, y, width, height, Text.empty(), (current - min) / (max - min));
        this.name = name;
        this.min = min;
        this.max = max;
        this.onChange = onChange;
        updateMessage();
    }

    public String name() {
        return name;
    }

    public double real() {
        return Math.round((min + value * (max - min)) * 100.0) / 100.0;
    }

    @Override
    protected void updateMessage() {
        if (name == null) return;
        setMessage(Text.literal(name + " = " + Format.compact(real())));
    }

    @Override
    protected void applyValue() {
        onChange.accept(real());
    }
}