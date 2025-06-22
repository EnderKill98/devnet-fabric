package devs.the.devnet_fabric;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PullCommand {
    // Global list of subcommands for /pull
    public static final List<String> DESTINATIONS = new ArrayList<>();

    public static LiteralArgumentBuilder<FabricClientCommandSource> create() {
        return ClientCommandManager.literal("pull")
            .executes(ctx -> {
                ctx.getSource().sendError(Text.literal("§cPlease specify a destination!"));
                return 0;
            })
                .then(ClientCommandManager.argument("destination", StringArgumentType.greedyString())
                        .suggests((ctx, suggestBuilder) -> CommandSource.suggestMatching(DESTINATIONS.stream(), suggestBuilder))
                        .executes((ctx) -> {
                            String destination = StringArgumentType.getString(ctx, "destination");
                            DevNetClient.getInstance().sendMessage(new Messaging.PullRequest(destination, 0, MinecraftClient.getInstance().player.getUuid()));
                            return 0;
                        })
                );
    }
}

