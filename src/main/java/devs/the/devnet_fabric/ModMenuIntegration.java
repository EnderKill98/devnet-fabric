package devs.the.devnet_fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.text.Text;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parentScreen -> YetAnotherConfigLib.createBuilder()
                .title(Text.literal("DevNet Fabric Client"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("DevNet"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Mod Active"))
                                .description(OptionDescription.of(Text.literal("Whether ads should be blocked and remote lists be periodically pulled (if enabled)")))
                                .controller(TickBoxControllerBuilder::create)
                                .binding(Config.HANDLER.defaults().modActive, () -> Config.HANDLER.instance().modActive, (newVal) -> Config.HANDLER.instance().modActive = newVal)
                                .build()
                        )
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Debug"))
                                .description(OptionDescription.of(Text.literal("Enable additional debug messages in logs for troubleshooting.")))
                                .controller(TickBoxControllerBuilder::create)
                                .binding(Config.HANDLER.defaults().debug, () -> Config.HANDLER.instance().debug, (newVal) -> Config.HANDLER.instance().debug = newVal)
                                .build()
                        )
                        .option(Option.<String>createBuilder()
                                .name(Text.literal("Secret Token"))
                                .binding(Config.HANDLER.defaults().token, () -> Config.HANDLER.instance().token.isBlank() ? "" : "<redacted>", (newVal) -> Config.HANDLER.instance().token = newVal)
                                .description(OptionDescription.of(Text.literal("Your private token. Do not share this!")))
                                .controller(StringControllerBuilder::create)
                                .build()
                        )
                        .option(Option.<String>createBuilder()
                                .name(Text.literal("Bot Feedback Format"))
                                .binding(Config.HANDLER.defaults().botFeedbackFormat, () -> Config.HANDLER.instance().botFeedbackFormat, (newVal) -> Config.HANDLER.instance().botFeedbackFormat = newVal)
                                .description(OptionDescription.of(Text.literal("""
                                    Message format to be used for bot feedback.
                                    You can use color codes with an ampersand as standing (&1, &l, ...).
                                    
                                    Variables:
                                     - §l{prefix}§r: Mod Prefix
                                     - §l{sender}§r: The name of the sender. Usually the name of the bot. But could be anything.
                                     - §l{destination}§r: The destination of the originating message.
                                     - §l{sender_or_destination}§r: Sender unless not present, then destination
                                     - §l{message}§r: The message itself.
                                    """)))
                                .controller(StringControllerBuilder::create)
                                .build()
                        )
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Actions"))
                                .option(ButtonOption.createBuilder()
                                        .name(Text.literal("Reconnect"))
                                        .description(OptionDescription.of(Text.literal("Reconnect to the backend now")))
                                        .action((_screen, _opt) -> DevNetClient.getInstance().reconnect())
                                        .build()
                                )
                                .build())
                        .build()
                )
                .save(() -> {
                    Config.HANDLER.save();
                    DevNetClient.getInstance().reconnect();
                })
                .build()
                .generateScreen(parentScreen);
    }

}
