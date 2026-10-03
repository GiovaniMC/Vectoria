package vectoria.modid.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class VectoriaScreen extends Screen {

    public VectoriaScreen() {
        super(Text.literal("vectoria"));
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("2D"), button -> client.setScreen(new Plot2DScreen(this)))
                .dimensions(width / 2 - 105, height / 2 - 10, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("3D"), button -> client.setScreen(new Plot3DScreen(this)))
                .dimensions(width / 2 + 5, height / 2 - 10, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 40, 0xFFFFFFFF);
    }
}