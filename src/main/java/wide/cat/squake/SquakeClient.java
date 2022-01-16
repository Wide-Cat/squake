package wide.cat.squake;

import net.fabricmc.api.ClientModInitializer;

public class SquakeClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ToggleKeyHandler.setup();
    }
}
