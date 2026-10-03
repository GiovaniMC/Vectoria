package vectoria.modid.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import vectoria.modid.client.gui.VectoriaScreen;

public class VectoriaClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		VectoriaKeybinds.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (VectoriaKeybinds.OPEN_SCREEN.wasPressed()) {
				client.setScreen(new VectoriaScreen());
			}
		});
	}
}