package app.ghostclient.mod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fala com o backend do Ghost Client para: amigos (pedir/aceitar/remover/listar),
 * presenca ("estou online agora") e chat (enviar/receber mensagens).
 *
 * Nao ha token de sessao partilhado entre o site e o jogo, por isso -- tal como o
 * lookup das capas -- tudo e identificado pelo UUID do jogador. Isto e o mesmo
 * modelo de confianca que o endpoint de capas ja usa.
 *
 * O chat funciona por polling (a cada ~2s), nao websocket puro: e simples, aguenta
 * bem reconexoes/lag de servidor Minecraft, e da uma sensacao "quase-tempo-real"
 * mais que suficiente para chat dentro do jogo. Pode evoluir para websocket depois.
 */
public final class FriendsClient {
    private static final Logger LOG = LoggerFactory.getLogger("ghostclient");

    private static final String BASE = System.getProperty(
            "ghostclient.social", "https://mc-social-core.lovable.app/api/public/gc/api");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public record Friend(UUID uuid, String username, boolean online, boolean ghost, long lastSeen) {}
    public record Request(UUID uuid, String username) {}
    public record ChatMessage(String id, UUID from, UUID to, String message, long ts, boolean mine) {}

    private static ScheduledExecutorService scheduler;
    private static volatile UUID self;
    private static volatile String selfName = "";

    private static final List<Friend> FRIENDS = new CopyOnWriteArrayList<>();
    private static final List<Request> INCOMING = new CopyOnWriteArrayList<>();
    private static final List<Request> OUTGOING = new CopyOnWriteArrayList<>();
    private static final Map<UUID, List<ChatMessage>> CHAT = new ConcurrentHashMap<>();
    private static final AtomicLong CHAT_SINCE = new AtomicLong(0);
    private static volatile String lastError = null;

    private FriendsClient() {}

    public static void start() {
        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "ghostclient-friends");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(FriendsClient::pingPresence, 2, 15, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(FriendsClient::refreshFriends, 3, 5, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(FriendsClient::pollChat, 4, 2, TimeUnit.SECONDS);
    }

    private static UUID selfUuid() {
        if (self != null) return self;
        UUID id = GhostPlayers.ownUuid();
        if (id != null) {
            self = id;
            selfName = GhostPlayers.ownName();
        }
        return self;
    }

    // ------------------------------------------------------------- leituras para a GUI

    public static List<Friend> friends() {
        List<Friend> out = new ArrayList<>(FRIENDS);
        out.sort(Comparator.comparing(Friend::online).reversed().thenComparing(Friend::username, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public static List<Request> incoming() { return new ArrayList<>(INCOMING); }
    public static List<Request> outgoing() { return new ArrayList<>(OUTGOING); }

    public static List<ChatMessage> chatWith(UUID friend) {
        return new ArrayList<>(CHAT.getOrDefault(friend, List.of()));
    }

    public static String lastError() { return lastError; }

    // ------------------------------------------------------------- acoes da GUI

    public static void sendFriendRequest(String username) {
        UUID me = selfUuid();
        if (me == null || username == null || username.isBlank()) return;
        submit(() -> post("/friends/request", body(Map.of(
                "uuid", strip(me), "username", selfName, "target_username", username.trim()))));
    }

    public static void acceptFriendRequest(UUID from) {
        UUID me = selfUuid();
        if (me == null || from == null) return;
        submit(() -> post("/friends/accept", body(Map.of("uuid", strip(me), "from_uuid", strip(from)))));
    }

    public static void declineFriendRequest(UUID from) {
        UUID me = selfUuid();
        if (me == null || from == null) return;
        submit(() -> post("/friends/decline", body(Map.of("uuid", strip(me), "from_uuid", strip(from)))));
    }

    public static void removeFriend(UUID friend) {
        UUID me = selfUuid();
        if (me == null || friend == null) return;
        FRIENDS.removeIf(f -> f.uuid().equals(friend));
        submit(() -> post("/friends/remove", body(Map.of("uuid", strip(me), "friend_uuid", strip(friend)))));
    }

    public static void sendMessage(UUID to, String text) {
        UUID me = selfUuid();
        if (me == null || to == null || text == null || text.isBlank()) return;
        String msg = text.length() > 500 ? text.substring(0, 500) : text;
        ChatMessage optimistic = new ChatMessage("local-" + System.nanoTime(), me, to, msg, System.currentTimeMillis(), true);
        CHAT.computeIfAbsent(to, k -> new CopyOnWriteArrayList<>()).add(optimistic);
        submit(() -> post("/chat/send", body(Map.of("from_uuid", strip(me), "to_uuid", strip(to), "message", msg))));
    }

    // ------------------------------------------------------------- polling

    private static void pingPresence() {
        UUID me = selfUuid();
        if (me == null) return;
        try {
            post("/presence/ping", body(Map.of("uuid", strip(me), "username", selfName)));
        } catch (Throwable t) {
            lastError = t.toString();
        }
    }

    private static void refreshFriends() {
        UUID me = selfUuid();
        if (me == null) return;
        try {
            String res = get("/friends/list?uuid=" + strip(me));
            JsonObject root = JsonParser.parseString(res).getAsJsonObject();

            List<Friend> friends = new ArrayList<>();
            if (root.has("friends") && root.get("friends").isJsonArray()) {
                for (JsonElement e : root.getAsJsonArray("friends")) {
                    JsonObject o = e.getAsJsonObject();
                    friends.add(new Friend(
                            uuid(o, "uuid"),
                            str(o, "username", "?"),
                            bool(o, "online"),
                            bool(o, "ghost"),
                            o.has("lastSeen") && !o.get("lastSeen").isJsonNull() ? o.get("lastSeen").getAsLong() : 0));
                }
            }
            List<Request> incoming = readRequests(root, "incoming");
            List<Request> outgoing = readRequests(root, "outgoing");

            FRIENDS.clear();
            FRIENDS.addAll(friends);
            INCOMING.clear();
            INCOMING.addAll(incoming);
            OUTGOING.clear();
            OUTGOING.addAll(outgoing);
            lastError = null;
        } catch (Throwable t) {
            lastError = t.toString();
            LOG.debug("refreshFriends falhou: {}", t.toString());
        }
    }

    private static List<Request> readRequests(JsonObject root, String key) {
        List<Request> out = new ArrayList<>();
        if (root.has(key) && root.get(key).isJsonArray()) {
            for (JsonElement e : root.getAsJsonArray(key)) {
                JsonObject o = e.getAsJsonObject();
                out.add(new Request(uuid(o, "uuid"), str(o, "username", "?")));
            }
        }
        return out;
    }

    private static void pollChat() {
        UUID me = selfUuid();
        if (me == null) return;
        try {
            long since = CHAT_SINCE.get();
            String res = get("/chat/poll?uuid=" + strip(me) + "&since=" + since);
            JsonObject root = JsonParser.parseString(res).getAsJsonObject();
            if (!root.has("messages") || !root.get("messages").isJsonArray()) return;
            long newest = since;
            for (JsonElement e : root.getAsJsonArray("messages")) {
                JsonObject o = e.getAsJsonObject();
                UUID from = uuid(o, "from");
                UUID to = uuid(o, "to");
                long ts = o.has("ts") && !o.get("ts").isJsonNull() ? o.get("ts").getAsLong() : System.currentTimeMillis();
                boolean mine = me.equals(from);
                UUID other = mine ? to : from;
                if (other == null) continue;
                ChatMessage msg = new ChatMessage(str(o, "id", "?"), from, to, str(o, "message", ""), ts, mine);
                List<ChatMessage> list = CHAT.computeIfAbsent(other, k -> new CopyOnWriteArrayList<>());
                boolean dup = list.stream().anyMatch(m -> m.id().equals(msg.id()));
                if (!dup) {
                    list.removeIf(m -> m.mine() && m.id().startsWith("local-") && m.message().equals(msg.message()));
                    list.add(msg);
                }
                if (ts > newest) newest = ts;
            }
            CHAT_SINCE.set(newest);
        } catch (Throwable t) {
            LOG.debug("pollChat falhou: {}", t.toString());
        }
    }

    // ------------------------------------------------------------- http helpers

    private static void submit(Runnable r) {
        if (scheduler != null) scheduler.execute(r);
        else r.run();
    }

    private static void post(String path, String jsonBody) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "GhostClientMod/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() / 100 != 2) throw new IllegalStateException("HTTP " + res.statusCode() + ": " + res.body());
        } catch (Exception e) {
            lastError = e.toString();
            LOG.debug("POST {} falhou: {}", path, e.toString());
        }
    }

    private static String get(String pathAndQuery) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + pathAndQuery))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "GhostClientMod/1.0")
                .GET()
                .build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() / 100 != 2) throw new IllegalStateException("HTTP " + res.statusCode());
        return res.body();
    }

    private static String body(Map<String, String> fields) {
        JsonObject o = new JsonObject();
        fields.forEach(o::addProperty);
        return o.toString();
    }

    private static String strip(UUID u) { return u.toString().replace("-", ""); }

    private static UUID uuid(JsonObject o, String field) {
        if (!o.has(field) || o.get(field).isJsonNull()) return null;
        String s = o.get(field).getAsString();
        try {
            if (s.length() == 32) {
                return UUID.fromString(s.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
            }
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static String str(JsonObject o, String field, String def) {
        return o.has(field) && !o.get(field).isJsonNull() ? o.get(field).getAsString() : def;
    }

    private static boolean bool(JsonObject o, String field) {
        return o.has(field) && !o.get(field).isJsonNull() && o.get(field).getAsBoolean();
    }
}
