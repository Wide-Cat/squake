package wide.cat.squake.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;

import java.io.*;

public class ConfigHandler {
    private static final File configDir = new File(MinecraftClient.getInstance().runDirectory, "config/squake");
    private static final File configFile = new File(configDir, "config.json");
    private static final ConfigValues configValues = ConfigHandler.loadConfigValues();

    private static ConfigValues loadConfigValues() {
        ConfigValues configValues1;
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        if (!configFile.exists()) {
            configValues1 = new ConfigValues();
        } else {
            try {
                configValues1 = new Gson().fromJson(new FileReader(configFile), ConfigValues.class);
            }
            catch (FileNotFoundException e) {
                configValues1 = new ConfigValues();
                e.printStackTrace();
            }
        }
        return configValues1;
    }

    public static void save() {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));
            new GsonBuilder().setPrettyPrinting().create().toJson(configValues, writer);
            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ConfigValues getConfigValues() {
        return configValues;
    }

    public static void setEnabled(boolean enabled) {
        configValues.enabled = enabled;
        save();
    }
}
