package pl.nks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    public String item = "minecraft:diamond";
    public int amount = 64;
    public int price = 1400;
    public String webhook = "";
    /** komenda wystawiania, bez ukosnika */
    public String sellCommand = "/wystaw";
    public boolean hasChest = false;
    public int chestX, chestY, chestZ;
    /** przerwa miedzy wystawieniami (20 ticków = 1 sekunda) */
    public int delayTicks = 40;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Config INSTANCE = load();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("nks.json");
    }

    public static Config load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                Config c = GSON.fromJson(Files.readString(p), Config.class);
                if (c != null) return c;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Config c = new Config();
        c.save();
        return c;
    }

    public void save() {
        try {
            Files.writeString(path(), GSON.toJson(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
