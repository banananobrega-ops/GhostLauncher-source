package app.ghostclient.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Pergunta ao backend do Ghost Client, por UUID, "este jogador usa o Ghost Client?" e
 * "qual e a capa ativa dele?". Guarda tudo em cache e vai buscar as capas a medida que
 * os jogadores aparecem.
 */
public final class GhostPlayers {
    private static final Logger LOG = LoggerFactory.getLogger("ghostclient");

    private static final String LOOKUP_URL = System.getProperty(
            "ghostclient.lookup",
            "https://mc-social-core.lovable.app/api/public/gc/api/players/lookup");

    private static final long TTL_MS = 120_000;
    private static final int BATCH = 50;
    private static final String BADGE_CHAR = "\uE000";

    private record Info(boolean ghost, String capeUrl, long fetchedAt) {}

    private static final Map<UUID, Info> INFO = new ConcurrentHashMap<>();
    private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private static final Map<String, Identifier> CAPES_READY = new ConcurrentHashMap<>();
    private static final Set<String> CAPES_LOADING = ConcurrentHashMap.newKeySet();
    private static final Map<Identifier, AssetInfo.TextureAssetInfo> ASSETS = new ConcurrentHashMap<>();

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final AtomicBoolean SEEN_CAPE_HOOK = new AtomicBoolean();
    private static final AtomicBoolean SEEN_BADGE_HOOK = new AtomicBoolean();
    private static volatile Method sessionProfileGetter;

    private static ScheduledExecutorService scheduler;
    private static volatile Text badge;

    private GhostPlayers() {}

    public static void start() {
        badge = buildBadge();
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "ghostclient-net");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(GhostPlayers::flush, 1, 1, TimeUnit.SECONDS);
        LOG.info("Ghost Client mod ativo (lookup: {})", LOOKUP_URL);
    }

    // ------------------------------------------------------------------ identidade

    /**
     * UUID a usar para perguntar ao backend. Para o TEU jogador usa o UUID da tua conta
     * (o da sessao), porque em servers offline-mode o UUID que o jogo te da e outro.
     */
    public static UUID idFor(AbstractClientPlayerEntity player) {
        if (player instanceof ClientPlayerEntity) {
            UUID own = sessionUuid();
            if (own != null) return own;
        }
        return player.getUuid();
    }

    /** UUID da tua propria conta (usa a mesma reflexao defensiva do resto do ficheiro). */
    public static UUID ownUuid() {
        return sessionUuid();
    }

    /** Nome da tua propria conta, ou "" se ainda nao estiver disponivel. */
    public static String ownName() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            Method m = sessionProfileGetter;
            if (m == null) {
                for (Method cand : MinecraftClient.class.getMethods()) {
                    if (cand.getParameterCount() == 0 && cand.getReturnType() == GameProfile.class) {
                        m = cand;
                        sessionProfileGetter = m;
                        break;
                    }
                }
            }
            if (m == null) return "";
            GameProfile gp = (GameProfile) m.invoke(mc);
            return gp != null && gp.name() != null ? gp.name() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    private static UUID sessionUuid() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            Method m = sessionProfileGetter;
            if (m == null) {
                for (Method cand : MinecraftClient.class.getMethods()) {
                    if (cand.getParameterCount() == 0 && cand.getReturnType() == GameProfile.class) {
                        m = cand;
                        sessionProfileGetter = m;
                        break;
                    }
                }
            }
            if (m == null) return null;
            GameProfile gp = (GameProfile) m.invoke(mc);
            return gp != null ? gp.id() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------------ lookup

    private static Info info(UUID id) {
        // So jogadores reais (UUID v4). Ignora NPCs e contas offline.
        if (id == null || id.version() != 4) return null;
        Info i = INFO.get(id);
        long now = System.currentTimeMillis();
        if ((i == null || now - i.fetchedAt > TTL_MS) && !IN_FLIGHT.contains(id)) {
            PENDING.add(id);
        }
        return i;
    }

    private static void flush() {
        try {
            if (PENDING.isEmpty()) return;
            List<UUID> batch = new ArrayList<>();
            Iterator<UUID> it = PENDING.iterator();
            while (it.hasNext() && batch.size() < BATCH) {
                UUID u = it.next();
                it.remove();
                if (IN_FLIGHT.add(u)) batch.add(u);
            }
            if (batch.isEmpty()) return;
            try {
                lookup(batch);
            } finally {
                batch.forEach(IN_FLIGHT::remove);
            }
        } catch (Throwable t) {
            LOG.warn("lookup falhou: {}", t.toString());
        }
    }

    private static void lookup(List<UUID> batch) {
        long now = System.currentTimeMillis();
        try {
            JsonArray arr = new JsonArray();
            for (UUID u : batch) arr.add(u.toString().replace("-", ""));
            JsonObject body = new JsonObject();
            body.add("uuids", arr);

            HttpRequest req = HttpRequest.newBuilder(URI.create(LOOKUP_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "GhostClientMod/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() / 100 != 2) throw new IllegalStateException("HTTP " + res.statusCode());

            JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
            JsonObject players = root.has("players") && root.get("players").isJsonObject()
                    ? root.getAsJsonObject("players") : new JsonObject();

            int ghosts = 0, capes = 0;
            for (UUID u : batch) {
                JsonElement e = players.get(u.toString().replace("-", ""));
                if (e == null) e = players.get(u.toString());
                boolean ghost = false;
                String url = null;
                if (e != null && e.isJsonObject()) {
                    JsonObject o = e.getAsJsonObject();
                    ghost = o.has("ghost") && !o.get("ghost").isJsonNull() && o.get("ghost").getAsBoolean();
                    if (o.has("capeUrl") && !o.get("capeUrl").isJsonNull()) url = o.get("capeUrl").getAsString();
                }
                if (ghost) ghosts++;
                if (url != null) capes++;
                INFO.put(u, new Info(ghost, url, now));
            }
            LOG.info("lookup: {} jogador(es) -> {} com Ghost, {} com capa", batch.size(), ghosts, capes);
        } catch (Exception ex) {
            LOG.warn("lookup falhou ({}): {}", LOOKUP_URL, ex.toString());
            // tenta outra vez daqui a ~30 s, mantendo o que ja sabiamos
            for (UUID u : batch) {
                Info old = INFO.get(u);
                INFO.put(u, new Info(old != null && old.ghost, old != null ? old.capeUrl : null, now - TTL_MS + 30_000));
            }
        }
    }

    // ------------------------------------------------------------------- capas

    /** Textura da capa do jogador, ou null (sem capa Ghost / ainda a carregar). */
    public static Identifier capeFor(UUID id) {
        if (SEEN_CAPE_HOOK.compareAndSet(false, true)) LOG.info("hook da capa a funcionar (a perguntar por {})", id);
        Info i = info(id);
        if (i == null || !i.ghost || i.capeUrl == null) return null;
        return capeTexture(i.capeUrl);
    }

    /** Devolve os SkinTextures do jogador com a capa (e o elytra) trocados pela nossa. */
    public static SkinTextures withCape(SkinTextures orig, Identifier cape) {
        if (orig == null || cape == null) return orig;
        AssetInfo.TextureAssetInfo asset = ASSETS.computeIfAbsent(cape, c -> new AssetInfo.TextureAssetInfo(c, c));
        try {
            if (orig.cape() != null && cape.equals(orig.cape().id())) return orig;
        } catch (Throwable ignored) {
            // Yarn/Sodium pode expor o asset com outro nome; recria na mesma.
        }
        return new SkinTextures(orig.body(), asset, asset, orig.model(), orig.secure());
    }

    private static Identifier capeTexture(String url) {
        Identifier ready = CAPES_READY.get(url);
        if (ready != null) return ready;
        if (url.startsWith("https://") && CAPES_LOADING.add(url)) {
            scheduler.execute(() -> download(url));
        }
        return null;
    }

    private static void download(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "GhostClientMod/1.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> res = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() / 100 != 2) throw new IllegalStateException("HTTP " + res.statusCode());
            byte[] raw = res.body();
            if (raw.length == 0 || raw.length > 2_000_000) throw new IllegalStateException("tamanho invalido");

            Identifier id = Identifier.of("ghostclient", "capes/" + sha1(url));
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> {
                NativeImage image = null;
                try {
                    // NativeImage + texture manager TEM de correr no render thread.
                    // Nao usamos java.awt (ImageIO/Graphics2D): mistura AWT com o
                    // contexto OpenGL do Sodium e rebenta o driver em AMD/Intel.
                    image = NativeImage.read(new ByteArrayInputStream(raw));
                    NativeImage cape = padCape(image);
                    if (cape != image) {
                        image.close();
                        image = cape;
                    }
                    AbstractTexture texture = makeTexture(image, id.toString());
                    image = null;
                    mc.getTextureManager().registerTexture(id, texture);
                    CAPES_READY.put(url, id);
                } catch (Throwable t) {
                    LOG.warn("nao consegui registar a capa: {}", t.toString());
                    if (image != null) {
                        try { image.close(); } catch (Throwable ignored) {}
                    }
                    retryLater(url);
                }
            });
        } catch (Throwable t) {
            LOG.warn("nao consegui descarregar a capa: {}", t.toString());
            retryLater(url);
        }
    }

    private static void retryLater(String url) {
        scheduler.schedule(() -> CAPES_LOADING.remove(url), 60, TimeUnit.SECONDS);
    }

    /**
     * O render da capa espera 64x32 (ou multiplos, ex. 128x64). Capas OptiFine 46x22 e
     * 22x17 sao colocadas no canto de uma imagem 64x32 transparente.
     */
    private static byte[] normalize(byte[] raw) throws Exception {
        BufferedImage img;
        try {
            img = ImageIO.read(new ByteArrayInputStream(raw));
        } catch (Throwable t) {
            return raw; // sem ImageIO: usa tal e qual
        }
        if (img == null) throw new IllegalStateException("PNG invalido");
        int w = img.getWidth(), h = img.getHeight();
        if ((w == 46 && h == 22) || (w == 22 && h == 17)) {
            BufferedImage out = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            img = out;
            w = 64;
            h = 32;
        }
        if (w % 64 != 0 || h * 2 != w) throw new IllegalStateException("tamanho de capa invalido: " + w + "x" + h);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    /** O construtor de NativeImageBackedTexture mudou varias vezes entre versoes: tenta as formas conhecidas. */
    private static AbstractTexture makeTexture(NativeImage image, String name) throws Exception {
        for (Constructor<?> c : NativeImageBackedTexture.class.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            Object[] args = null;
            if (p.length == 1 && p[0] == NativeImage.class) {
                args = new Object[]{image};
            } else if (p.length == 2 && p[1] == NativeImage.class && p[0] == Supplier.class) {
                Supplier<String> label = () -> name;
                args = new Object[]{label, image};
            } else if (p.length == 2 && p[1] == NativeImage.class && p[0] == String.class) {
                args = new Object[]{name, image};
            }
            if (args != null) {
                c.setAccessible(true);
                return (AbstractTexture) c.newInstance(args);
            }
        }
        throw new IllegalStateException("NativeImageBackedTexture: construtor nao encontrado");
    }

    private static String sha1(String s) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ------------------------------------------------------------------- badge

    private static Text buildBadge() {
        try {
            JsonElement json = JsonParser.parseString(
                    "{\"text\":\"\\uE000\",\"font\":\"ghostclient:badge\",\"color\":\"white\"}");
            return TextCodecs.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(null);
        } catch (Throwable t) {
            LOG.warn("nao consegui criar o badge: {}", t.toString());
            return null;
        }
    }

    /** Poe a logo do Ghost a esquerda do nome, se o jogador usar o Ghost Client. */
    public static Text withBadge(UUID id, Text name) {
        Text b = badge;
        if (b == null || name == null) return name;
        if (SEEN_BADGE_HOOK.compareAndSet(false, true)) LOG.info("hook do badge a funcionar (a perguntar por {})", id);
        Info i = info(id);
        if (i == null || !i.ghost) return name;
        if (name.getString().startsWith(BADGE_CHAR)) return name; // ja tem
        return Text.empty().append(b).append(Text.literal(" ")).append(name);
    }
}
