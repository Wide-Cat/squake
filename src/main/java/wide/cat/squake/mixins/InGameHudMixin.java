package wide.cat.squake.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wide.cat.squake.config.ConfigHandler;
import wide.cat.squake.config.ConfigValues;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    private final ConfigValues configValues = ConfigHandler.getConfigValues();

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender2d(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (client == null || client.player == null || client.world == null) return;
        if (!configValues.drawSpeed) return;

        client.getProfiler().push("squake-render-2d");

        client.textRenderer.drawWithShadow(matrices, getSpeedString(), configValues.xPosition, client.getWindow().getScaledHeight() - client.textRenderer.fontHeight - configValues.yPosition, 0xAFAFAFDD);

        client.getProfiler().pop();
    }

    private String getSpeedString() {
        double tX = Math.abs(client.player.getX() - client.player.prevX);
        double tZ = Math.abs(client.player.getZ() - client.player.prevZ);
        double length = Math.sqrt(tX * tX + tZ * tZ);
        double bps = length * 20;

        return String.format("%.2f", bps);
    }
}
