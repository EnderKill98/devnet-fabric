package devs.the.devnet_fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevNetMod implements ClientModInitializer {

    public static Logger LOGGER = LoggerFactory.getLogger("devnet");

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(PullCommand.create());
        });

        Config.HANDLER.load();
        DevNetClient.getInstance();

        /*
        Messaging.BotFeedback feedback = new Messaging.BotFeedback(true, "EnderKill98", "test-base", "You stupid!");
        Messaging.CheckResponse cresp = new Messaging.CheckResponse("test-base-2", UUID.randomUUID(), List.of(new JsonObject(), new JsonObject()));
        String feedbackJson = Messaging.PRETTY_GSON.toJson(feedback);
        String crespJson = Messaging.PRETTY_GSON.toJson(cresp);
        LOGGER.info("Feedback JSON: {}", feedbackJson);
        LOGGER.info("CheckResponse JSON: {}", crespJson);

        Messaging.Message feedbackMsg = Messaging.GSON.fromJson(feedbackJson, Messaging.Message.class);
        Messaging.Message crespMsg = Messaging.GSON.fromJson(crespJson, Messaging.Message.class);
        LOGGER.info("Feedback Deserialized: {}", feedbackMsg);
        LOGGER.info("CheckResponse Deserialized: {}", crespMsg);
        */
    }
}
