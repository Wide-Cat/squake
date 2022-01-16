package wide.cat.squake.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

public class SquakeModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return new ConfigScreen();
    }

    static class ConfigScreen implements ConfigScreenFactory<Screen> {
        ConfigScreen() {}

        public Screen create(Screen screen) {
            return new ModConfigScreen(screen, ConfigHandler.getConfigValues()).getScreen();
        }
    }
}
