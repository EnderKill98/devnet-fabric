package devs.the.devnet_fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

public class CheckPearlsCommand {
    public static LiteralArgumentBuilder<FabricClientCommandSource> create() {
        return ClientCommandManager.literal("checkpearls")
            .executes(ctx -> {
                ctx.getSource().sendError(Text.literal("§cPlease specify a destination!"));
                return 0;
            })
                .then(ClientCommandManager.argument("destination", StringArgumentType.string())
                        .suggests((ctx, suggestBuilder) -> CommandSource.suggestMatching(DevNetMod.DESTINATIONS.stream(), suggestBuilder))
                        .executes((ctx) -> {
                            String destination = StringArgumentType.getString(ctx, "destination");
                            DevNetClient.getInstance().sendMessage(new Messaging.CheckRequest(destination, MinecraftClient.getInstance().player.getUuid()));
                            return 0;
                        })
                );
    }
}

