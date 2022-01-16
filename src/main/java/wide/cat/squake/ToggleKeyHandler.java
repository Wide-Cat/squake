package wide.cat.squake;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.impl.client.keybinding.KeyBindingRegistryImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.TranslatableText;
import org.lwjgl.glfw.GLFW;
import wide.cat.squake.config.ConfigHandler;
import wide.cat.squake.config.ConfigValues;


@Environment(EnvType.CLIENT)
public class ToggleKeyHandler {
    private static final KeyBinding TOGGLE_KEY = new KeyBinding("key.squake.toggle", GLFW.GLFW_KEY_COMMA, "key.categories.squake");

    public static void setup() {
        KeyBindingRegistryImpl.registerKeyBinding(TOGGLE_KEY);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (TOGGLE_KEY.wasPressed()) {
                ConfigValues configValues = ConfigHandler.getConfigValues();

                ConfigHandler.setEnabled(!configValues.enabled);
                if (configValues.enabled) MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new TranslatableText("key.squake.toggle.enabled"));
                else MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new TranslatableText("key.squake.toggle.disabled"));
            }
        });
    }


}
