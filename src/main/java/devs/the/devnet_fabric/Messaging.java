package devs.the.devnet_fabric;

import com.google.common.collect.HashBiMap;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class Messaging {

    public interface Message {
    }

    public static class MessageTypeAdapter <T extends Message> extends TypeAdapter<T> {
        private final Gson gson = new Gson();
        private final HashBiMap<String, Class<?>> typeClasses = HashBiMap.create();

        public MessageTypeAdapter<T> register(Class<?> clazz, String type) {
            if(!Message.class.isAssignableFrom(clazz))
                throw new IllegalArgumentException("The class \"" + clazz.getName() + "\" is not based on Message (type \"" + type + "\")!");
            typeClasses.put(type, clazz);
            return this;
        }

        @Override
        public void write(JsonWriter out, Message value) throws IOException {
            out.beginObject();

            String type = typeClasses.inverse().getOrDefault(value.getClass(), null);
            if(type == null)
                throw new IllegalArgumentException("Can't write class " + value.getClass() + ", as no type has been registered for it!");

            out.name("type").value(type);
            out.name("data");
            gson.toJson(value, value.getClass(), out);

            out.endObject();
        }

        @Override
        public T read(JsonReader in) throws IOException {
            JsonObject jsonObject = JsonParser.parseReader(in).getAsJsonObject();
            String type = jsonObject.get("type").getAsString();

            Class<?> clazz = typeClasses.getOrDefault(type, null);
            if(clazz == null) return null; // Type not found

            JsonElement data = jsonObject.get("data");

            Object obj = gson.fromJson(data, clazz);
            if(obj instanceof Message message) {
                return (T) message;
            }else {
                throw new IllegalArgumentException("Type \"" + type + "\" has a class which is not instanceof Message!");
            }
        }

        public HashBiMap<String, Class<?>> getTypeClasses() {
            return typeClasses;
        }
    }

    public static class MessageTypeAdapterFactory <T extends Message> implements TypeAdapterFactory {

        private final MessageTypeAdapter<T> messageTypeAdapter;

        public MessageTypeAdapterFactory(MessageTypeAdapter<T> messageTypeAdapter) {
            this.messageTypeAdapter = messageTypeAdapter;
        }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            Class<?> clazz = type.getRawType();
            if(!messageTypeAdapter.getTypeClasses().inverse().containsKey(clazz) && clazz != Message.class)
                return null;
            return (TypeAdapter<T>) messageTypeAdapter;
        }
    }

    public static final MessageTypeAdapterFactory<?> CONTENT_TYPE_ADAPTER_FACTORY = new MessageTypeAdapterFactory<>(
            new MessageTypeAdapter<>()
                    .register(DestinationsRequest.class, "destinations_request")
                    .register(DestinationsResponse.class, "destinations_response")
                    .register(CheckRequest.class, "check_request")
                    .register(CheckResponse.class, "check_response")
                    .register(PullRequest.class, "pull_request")
                    .register(BotFeedback.class, "bot_feedback")
    );
    public static final Gson GSON = new GsonBuilder().registerTypeAdapterFactory(CONTENT_TYPE_ADAPTER_FACTORY).create();
    public static final Gson PRETTY_GSON = new GsonBuilder().registerTypeAdapterFactory(CONTENT_TYPE_ADAPTER_FACTORY).setPrettyPrinting().create();

    public record Destination(String id, String botName, boolean reachable) {}

    public record DestinationsRequest() implements Message { }
    public record DestinationsResponse(List<Destination> destinations) implements Message { }
    public record CheckRequest(@NotNull String destination, @NotNull UUID forMcId) implements Message { }
    public record CheckResponse(@NotNull String destination, @NotNull UUID forMcId, @NotNull List<JsonObject> pearls) implements Message { }
    public record PullRequest(@NotNull String destination, int pearlIndex, @NotNull UUID forMcId) implements Message { }
    public record BotFeedback(@SerializedName("error") boolean isError, @NotNull String sender, @NotNull String destination, @NotNull String message) implements Message { }

}
