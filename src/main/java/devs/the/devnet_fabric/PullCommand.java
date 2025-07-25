package devs.the.devnet_fabric;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

public class PullCommand {
    public static LiteralArgumentBuilder<FabricClientCommandSource> create() {
        return ClientCommandManager.literal("pull")
            .executes(ctx -> {
                ctx.getSource().sendError(Text.literal("§cPlease specify a destination!"));
                return 0;
            })
                .then(ClientCommandManager.argument("destination", StringArgumentType.string())
                        .suggests((ctx, suggestBuilder) -> CommandSource.suggestMatching(DevNetMod.DESTINATIONS.stream(), suggestBuilder))
                        .executes((ctx) -> {
                            String destination = StringArgumentType.getString(ctx, "destination");
                            DevNetClient.getInstance().sendMessage(new Messaging.PullRequest(destination, 0, MinecraftClient.getInstance().player.getUuid()));
                            return 0;
                        })
                        .then(ClientCommandManager.argument("pearlIndex", IntegerArgumentType.integer())
                            .executes((ctx) -> {
                                String destination = StringArgumentType.getString(ctx, "destination");
                                int index = IntegerArgumentType.getInteger(ctx, "pearlIndex");
                                DevNetClient.getInstance().sendMessage(new Messaging.PullRequest(destination, index, MinecraftClient.getInstance().player.getUuid()));
                                return 0;
                            }
                            )
                )
                );
    }
}

