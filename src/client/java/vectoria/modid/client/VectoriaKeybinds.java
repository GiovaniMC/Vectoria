package vectoria.modid.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class VectoriaKeybinds {
    public static KeyBinding OPEN_SCREEN;

    public static void register() {
        OPEN_SCREEN = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.vectoria.open_screen",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_M,
                        "category.vectoria"
                )
        );
    }

    private VectoriaKeybinds() {
    }
}