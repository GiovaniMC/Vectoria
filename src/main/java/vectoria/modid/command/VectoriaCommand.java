package vectoria.modid.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class VectoriaCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("vectoria")
                            .then(argument("expression", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        String expression = StringArgumentType.getString(
                                                context,
                                                "expression"
                                        );

                                        context.getSource().sendFeedback(
                                                () -> net.minecraft.text.Text.literal(expression),
                                                false
                                        );

                                        return 1;
                                    }))
            );
        });
    }

    private VectoriaCommand() {
    }
}