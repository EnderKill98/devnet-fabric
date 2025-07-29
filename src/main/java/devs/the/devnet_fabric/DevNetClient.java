package devs.the.devnet_fabric;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DevNetClient {

    private static Logger LOGGER = LoggerFactory.getLogger("devnet/client");

    private static DevNetClient INSTANCE;

    public static DevNetClient getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new DevNetClient();
        }
        return INSTANCE;
    }

    private WebSocketClient webSocketClient;
    private final ScheduledExecutorService executorService;
    private final AtomicBoolean isConnecting;
    private final AtomicInteger reconnectAttempts;
    private final String serverUri;

    // Configuration constants
    private static final int INITIAL_RECONNECT_DELAY = 1000; // 1 second
    private static final int MAX_RECONNECT_DELAY = 30000; // 30 seconds
    private static final double BACKOFF_MULTIPLIER = 2.0;

    public DevNetClient() {
        this("wss://devnet.cosmos-ink.net/ws"); // Default URI, change as needed
    }

    public DevNetClient(String serverUri) {
        this.serverUri = serverUri;
        this.executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("DevNetClient");
            thread.setDaemon(true);
            return thread;
        });
        this.isConnecting = new AtomicBoolean(false);
        this.reconnectAttempts = new AtomicInteger(0);

        // Initialize connection
        connect();
    }

    private void connect() {
        if(!Config.HANDLER.instance().modActive) {
            LOGGER.info("Mod is not active. Aborted connection.");
            return;
        }
        if(Config.HANDLER.instance().token.isBlank()) {
            LOGGER.info("No token provided. Aborted connection.");
            return;
        }

        if (isConnecting.get()) {
            LOGGER.warn("Already attempting to connect...");
            return;
        }

        isConnecting.set(true);

        try {
            URI uri = new URI(serverUri);
            webSocketClient = new WebSocketClient(uri) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    LOGGER.info("WebSocket connection established");
                    isConnecting.set(false);
                    reconnectAttempts.set(0); // Reset reconnect attempts on successful connection
                    onConnectionEstablished();
                }

                @Override
                public void onMessage(String message) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.submit(() -> handleMessage(client, message));
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    LOGGER.info("WebSocket connection closed: " + reason + " (Code: " + code + ")");
                    isConnecting.set(false);
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    LOGGER.error("WebSocket error: " + ex.getMessage());
                    isConnecting.set(false);
                    if (!isOpen()) {
                        scheduleReconnect();
                    }
                }
            };

            // Connect in background thread
            executorService.execute(() -> {
                try {
                    webSocketClient.addHeader("Authorization", "User " + Config.HANDLER.instance().token);
                    webSocketClient.connect();
                } catch (Exception e) {
                    LOGGER.error("Failed to connect: " + e.getMessage());
                    isConnecting.set(false);
                    scheduleReconnect();
                }
            });

        } catch (URISyntaxException e) {
            LOGGER.error("Invalid URI: " + e.getMessage(), e);
            isConnecting.set(false);
        }
    }

    private void scheduleReconnect() {
        int attempts = reconnectAttempts.get();

        /*if (attempts >= MAX_RECONNECT_ATTEMPTS) {
            LOGGER.warn("Max reconnection attempts reached. Stopping reconnection.");
            return;
        }*/

        // Calculate delay with exponential backoff
        long delay = Math.min((long) (INITIAL_RECONNECT_DELAY * Math.pow(BACKOFF_MULTIPLIER, attempts)), MAX_RECONNECT_DELAY);

        reconnectAttempts.incrementAndGet();

        LOGGER.info("Scheduling reconnection attempt {} in {} ms", attempts + 1, delay);

        executorService.schedule(() -> {
            if (webSocketClient == null || !webSocketClient.isOpen()) {
                connect();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    public boolean sendMessage(Messaging.Message message) {
        return sendMessage(Messaging.GSON.toJson(message));
    }

    public boolean sendMessage(String message) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(message);
            if(Config.HANDLER.instance().debug) {
                LOGGER.info("Sent message: {}", message);
            }
            return true;
        } else {
            LOGGER.error("Cannot send message: WebSocket is not connected");
            return false;
        }
    }

    public boolean isConnected() {
        return webSocketClient != null && webSocketClient.isOpen();
    }

    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    public void shutdown() {
        disconnect();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Override these methods in subclasses or use listeners
    protected void onConnectionEstablished() {
        // Override to handle connection established
        sendMessage(new Messaging.DestinationsRequest());
    }

    protected void handleMessage(MinecraftClient client, String message) {
        // Override to handle incoming messages
        if(Config.HANDLER.instance().debug) {
            LOGGER.info("Received message: {}", message);
        }
        try {
            Messaging.Message msg = Messaging.GSON.fromJson(message, Messaging.Message.class);
            if(msg == null) {
                LOGGER.warn("Failed to deserialize message. Perhaps the type is unkown/unsupported: {}", message);
                return;
            }

            if(msg instanceof Messaging.DestinationsRequest || msg instanceof Messaging.CheckRequest || msg instanceof Messaging.PullRequest) {
                LOGGER.warn("Did not expect to receive a request: {}", msg);
            }else if(msg instanceof Messaging.DestinationsResponse destinationsResponse) {
                DevNetMod.DESTINATIONS.clear();
                for(Messaging.Destination destination : destinationsResponse.destinations())
                    DevNetMod.DESTINATIONS.add(destination.id());
                LOGGER.info("Received {} destinations: {}", DevNetMod.DESTINATIONS.size(), String.join(", ", DevNetMod.DESTINATIONS));
            }else if(msg instanceof Messaging.CheckResponse checkResponse) {
                StringBuilder chatMessage = new StringBuilder(DevNetMod.PREFIX + "§a Check Response from §2" + checkResponse.destination() + "§a: ");
                if (checkResponse.pearls().isEmpty()) {
                    chatMessage.append("§oNo pearls reported.");
                } else {
                    for(int pearlIndex = 0; pearlIndex < checkResponse.pearls().size(); pearlIndex++) {
                        chatMessage.append("\n §2" + pearlIndex + ":§a " + checkResponse.pearls().get(pearlIndex).toString());
                    }
                }
                if(client.inGameHud != null && client.inGameHud.getChatHud() != null)
                    client.inGameHud.getChatHud().addMessage(Text.literal(chatMessage.toString()));
                else {
                    LOGGER.info("Could not show bot checkl feedback: Sender: {}, ForMcId: {}, Pearls: {}", checkResponse.destination(), checkResponse.forMcId(), Messaging.PRETTY_GSON.toJson(checkResponse.pearls()));
                }

                LOGGER.info("Received check response (TODO, use it): {}", checkResponse);
            }else if(msg instanceof Messaging.BotFeedback botFeedback) {
                String chatMessage = Config.HANDLER.instance().botFeedbackFormat;
                for(char code : "0123456789abcdefklmnor".toCharArray())
                    chatMessage = chatMessage.replace("&" + code, "§" + code);
                chatMessage = chatMessage
                        .replace("{prefix}", DevNetMod.PREFIX)
                        .replace("{sender}", botFeedback.sender())
                        .replace("{destination}", botFeedback.destination())
                        .replace("{sender_or_destination}", botFeedback.sender().isBlank() ? botFeedback.destination() : botFeedback.sender())
                        .replace("{message}", botFeedback.message());

                if(client.inGameHud != null && client.inGameHud.getChatHud() != null)
                    client.inGameHud.getChatHud().addMessage(Text.literal(chatMessage));
                else
                    LOGGER.info("Could not show bot feedback: Sender: {}, Destination: {}, Message: {}", botFeedback.sender(), botFeedback.destination(), botFeedback.message());
            }else {
                LOGGER.warn("Unrecognized/implemented message type: {}", message);
            }
        }catch (Exception ex) {
            LOGGER.error("Error parsing message: " + ex.getMessage(), ex);
        }
    }

    // Optional: Add listener support
    public interface ConnectionListener {
        void onConnected();
        void onDisconnected();
        void onMessage(String message);
        void onError(Exception e);
    }

    private ConnectionListener connectionListener;

    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    public void reconnect() {
        reconnectAttempts.set(0);
        disconnect();
        connect();
    }
}