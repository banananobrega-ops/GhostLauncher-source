package app.ghostclient.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Definicoes simples do Ghost Client, guardadas em config/ghostclient.json.
 * Cada campo aqui aparece como um toggle no separador "Mods" do menu.
 */
public final class GhostConfig {
    private static final Logger LOG = LoggerFactory.getLogger("ghostclient");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ghostclient.json");

    public boolean showCapes = true;
    public boolean showBadges = true;
    public boolean showFriendsOnHud = true;
    public boolean chatPopupEnabled = true;

    private static GhostConfig instance;

    public static GhostConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static GhostConfig load() {
        try {
            if (Files.exists(FILE)) {
                String json = Files.readString(FILE, StandardCharsets.UTF_8);
                GhostConfig cfg = GSON.fromJson(json, GhostConfig.class);
                if (cfg != null) return cfg;
            }
        } catch (Exception e) {
            LOG.warn("nao consegui ler ghostclient.json: {}", e.toString());
        }
        return new GhostConfig();
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("nao consegui guardar ghostclient.json: {}", e.toString());
        }
    }
}
