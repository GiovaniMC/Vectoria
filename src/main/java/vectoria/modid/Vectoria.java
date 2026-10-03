package vectoria.modid;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vectoria.modid.command.VectoriaCommand;

public class Vectoria implements ModInitializer {
	public static final String MOD_ID = "vectoria";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			VectoriaCommand.register(dispatcher);

		});
		LOGGER.info("Vectoria math engine initialized successfully.");
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}