package vectoria.modid.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class VectoriaScreen extends Screen {
    public VectoriaScreen() {
        super(Text.literal("Vectoria"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Vectoria"),
                width / 2,
                height / 2,
                0xFFFFFF
        );

        super.render(context, mouseX, mouseY, delta);
    }
}