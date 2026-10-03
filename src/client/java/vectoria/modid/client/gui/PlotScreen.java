package vectoria.modid.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import vectoria.modid.plot.PlotFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public abstract class PlotScreen extends Screen {
    protected static final int PANEL_WIDTH = 220;
    private static final int FIELD_TOP = 30;
    private static final int FIELD_STEP = 34;
    private static final int SLIDER_STEP = 22;
    private static final double SLIDER_MIN = -10.0;
    private static final double SLIDER_MAX = 10.0;

    private final Screen parent;
    private final PlotFunction.Mode mode;
    private final int rows;
    private final String[] texts;
    private final String[] errors;
    private final List<TextFieldWidget> fields = new ArrayList<>();
    private final List<ParamSlider> sliders = new ArrayList<>();
    private final Map<String, Double> paramValues = new TreeMap<>();
    private int hiddenSliders;
    private boolean dragging;

    protected final PlotFunction[] functions;
    protected boolean dirty = true;

    protected PlotScreen(Text title, Screen parent, PlotFunction.Mode mode, int rows) {
        super(title);
        this.parent = parent;
        this.mode = mode;
        this.rows = rows;
        this.texts = new String[rows];
        this.errors = new String[rows];
        this.functions = new PlotFunction[rows];
        java.util.Arrays.fill(texts, "");
    }

    protected abstract void renderPlot(DrawContext context, int mouseX, int mouseY);

    protected abstract void resetView();

    protected abstract void onPlotDrag(double deltaX, double deltaY);

    protected abstract void onPlotScroll(double mouseX, double mouseY, double amount);

    protected abstract String hint(int index);

    protected abstract int rowColor(int index);

    protected int plotLeft() {
        return PANEL_WIDTH;
    }

    protected int plotWidth() {
        return width - PANEL_WIDTH;
    }

    protected Map<String, Double> params() {
        return paramValues;
    }

    protected boolean inPlot(double mouseX, double mouseY) {
        return mouseX >= plotLeft() && mouseX < width && mouseY >= 0 && mouseY < height;
    }

    private int sliderTop() {
        return FIELD_TOP + rows * FIELD_STEP + 24;
    }

    @Override
    protected void init() {
        fields.clear();
        sliders.clear();

        for (int i = 0; i < rows; i++) {
            final int index = i;
            TextFieldWidget field = new TextFieldWidget(
                    textRenderer, 20, FIELD_TOP + i * FIELD_STEP, PANEL_WIDTH - 30, 18,
                    Text.literal("Function " + (i + 1))
            );
            field.setMaxLength(200);
            field.setText(texts[i]);
            field.setSuggestion(texts[i].isEmpty() ? hint(i) : null);
            field.setChangedListener(value -> onTextChanged(index, value));
            addDrawableChild(field);
            fields.add(field);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Reset view"), button -> {
            resetView();
            dirty = true;
        }).dimensions(8, height - 50, PANEL_WIDTH - 16, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
                .dimensions(8, height - 26, PANEL_WIDTH - 16, 20).build());

        syncSliders(true);

        if (!fields.isEmpty()) setInitialFocus(fields.get(0));
        dirty = true;
    }

    private void onTextChanged(int index, String value) {
        texts[index] = value;
        fields.get(index).setSuggestion(value.isEmpty() ? hint(index) : null);

        if (value.trim().isEmpty()) {
            functions[index] = null;
            errors[index] = null;
        } else {
            try {
                functions[index] = PlotFunction.compile(value, mode);
                errors[index] = null;
            } catch (RuntimeException e) {
                errors[index] = e.getMessage() == null ? "Invalid expression" : e.getMessage();
            }
        }

        syncSliders(false);
        dirty = true;
    }

    private void syncSliders(boolean force) {
        TreeSet<String> needed = new TreeSet<>();
        for (PlotFunction function : functions) {
            if (function != null) needed.addAll(function.parameters());
        }
        for (String name : needed) {
            paramValues.putIfAbsent(name, 1.0);
        }

        int capacity = Math.max(0, (height - 70 - sliderTop()) / SLIDER_STEP);
        List<String> visible = new ArrayList<>(needed);
        hiddenSliders = Math.max(0, visible.size() - capacity);
        if (hiddenSliders > 0) visible = new ArrayList<>(visible.subList(0, capacity));

        List<String> current = new ArrayList<>();
        for (ParamSlider slider : sliders) current.add(slider.name());
        if (!force && visible.equals(current)) return;

        for (ParamSlider slider : sliders) remove(slider);
        sliders.clear();

        int y = sliderTop();
        for (String name : visible) {
            ParamSlider slider = new ParamSlider(
                    8, y, PANEL_WIDTH - 16, 18, name, SLIDER_MIN, SLIDER_MAX, paramValues.get(name),
                    value -> {
                        paramValues.put(name, value);
                        dirty = true;
                    }
            );
            addDrawableChild(slider);
            sliders.add(slider);
            y += SLIDER_STEP;
        }
        dirty = true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xFF101014);
        renderPlot(context, mouseX, mouseY);
        context.fill(0, 0, PANEL_WIDTH, height, 0xFF1B1B21);
        context.fill(PANEL_WIDTH - 1, 0, PANEL_WIDTH, height, 0xFF3A3A44);

        super.render(context, mouseX, mouseY, delta);

        context.drawText(textRenderer, getTitle(), 8, 10, 0xFFFFFFFF, false);

        for (int i = 0; i < rows; i++) {
            int y = FIELD_TOP + i * FIELD_STEP;
            context.fill(8, y + 3, 16, y + 15, rowColor(i));
            if (errors[i] != null) {
                String message = textRenderer.trimToWidth(errors[i], PANEL_WIDTH - 20);
                context.drawText(textRenderer, message, 10, y + 21, 0xFFFF6B6B, false);
            }
        }

        if (!sliders.isEmpty()) {
            context.drawText(textRenderer, "Parameters", 8, sliderTop() - 12, 0xFFA0A0AA, false);
        }
        if (hiddenSliders > 0) {
            context.drawText(textRenderer, "+" + hiddenSliders + " more parameters hidden", 8, height - 64, 0xFFFFC857, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && inPlot(mouseX, mouseY)) {
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        if (dragging && button == 0) {
            onPlotDrag(deltaX, deltaY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (inPlot(mouseX, mouseY)) {
            onPlotScroll(mouseX, mouseY, amount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}