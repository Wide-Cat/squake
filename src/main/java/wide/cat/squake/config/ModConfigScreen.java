package wide.cat.squake.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ModConfigScreen {
    private final Screen configScreen;

    public ModConfigScreen(Screen parent, ConfigValues configValues) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("title.squake.config"));
        ConfigCategory category = builder.getOrCreateCategory(Text.translatable("title.squake.general"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        category.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config_value.squake.enabled"), configValues.enabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("tooltip.squake.enabled"))
                .setSaveConsumer(newValue -> configValues.enabled = newValue)
                .build());
        category.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config_value.squake.sharkingEnabled"), configValues.sharkingEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("tooltip.squake.sharkingEnabled"))
                .setSaveConsumer(newValue -> configValues.sharkingEnabled = newValue)
                .build());
        category.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config_value.squake.trimpingEnabled"), configValues.trimpingEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("tooltip.squake.trimpingEnabled"))
                .setSaveConsumer(newValue -> configValues.trimpingEnabled = newValue)
                .build());
        category.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config_value.squake.uncappedBunnyhopEnabled"), configValues.uncappedBunnyhopEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("tooltip.squake.uncappedBunnyhopEnabled"))
                .setSaveConsumer(newValue -> configValues.uncappedBunnyhopEnabled = newValue)
                .build());

        category.addEntry(entryBuilder.startDoubleField(Text.translatable("config_value.squake.accelerate"), configValues.accelerate)
                .setDefaultValue(10.0)
                .setMin(0)
                .setMax(Double.MAX_VALUE)
                .setTooltip(Text.translatable("tooltip.squake.accelerate"))
                .setSaveConsumer(newValue -> configValues.accelerate = newValue)
                .build());
        category.addEntry(entryBuilder.startDoubleField(Text.translatable("config_value.squake.airAccelerate"), configValues.airAccelerate)
                .setDefaultValue(14.0)
                .setMin(0)
                .setMax(Double.MAX_VALUE)
                .setTooltip(Text.translatable("tooltip.squake.airAccelerate"))
                .setSaveConsumer(newValue -> configValues.airAccelerate = newValue)
                .build());
        category.addEntry(entryBuilder.startDoubleField(Text.translatable("config_value.squake.sharkingSurfTension"), configValues.sharkingSurfTension)
                .setDefaultValue(0.2)
                .setMin(0)
                .setMax(Double.MAX_VALUE)
                .setTooltip(Text.translatable("tooltip.squake.sharkingSurfTension"))
                .setSaveConsumer(newValue -> configValues.sharkingSurfTension = newValue)
                .build());
        category.addEntry(entryBuilder.startDoubleField(Text.translatable("config_value.squake.trimpMult"), configValues.trimpMult)
                .setDefaultValue(1.4)
                .setMin(0)
                .setMax(Double.MAX_VALUE)
                .setTooltip(Text.translatable("tooltip.squake.trimpMult"))
                .setSaveConsumer(newValue -> configValues.trimpMult = newValue)
                .build());
        category.addEntry(entryBuilder.startDoubleField(Text.translatable("config_value.squake.sharkingWaterFriction"), configValues.sharkingWaterFriction)
                .setDefaultValue(0.1)
                .setTooltip(Text.translatable("tooltip.squake.sharkingWaterFriction"))
                .setMin(0)
                .setMax(1)
                .setSaveConsumer(newValue -> configValues.sharkingWaterFriction = newValue)
                .build());

        category.addEntry(entryBuilder.startFloatField(Text.translatable("config_value.squake.maxAirAccelerationPerTick"), configValues.maxAirAccelerationPerTick)
                .setDefaultValue(0.045F)
                .setMin(0)
                .setMax(Float.MAX_VALUE)
                .setTooltip(Text.translatable("tooltip.squake.maxAirAccelerationPerTick"))
                .setSaveConsumer(newValue -> configValues.maxAirAccelerationPerTick = newValue)
                .build());
        category.addEntry(entryBuilder.startFloatField(Text.translatable("config_value.squake.softCap"), configValues.softCap)
                .setDefaultValue(1.4F)
                .setMin(0)
                .setMax(Float.MAX_VALUE)
                .setTooltip(Text.translatable("tooltip.squake.softCap"))
                .setSaveConsumer(newValue -> configValues.softCap = newValue)
                .build());
        category.addEntry(entryBuilder.startFloatField(Text.translatable("config_value.squake.hardCap"), configValues.hardCap)
                .setDefaultValue(2F)
                .setMin(0)
                .setMax(Float.MAX_VALUE)
                .setTooltip(Text.translatable("tooltip.squake.hardCap"))
                .setSaveConsumer(newValue -> configValues.hardCap = newValue)
                .build());
        category.addEntry(entryBuilder.startFloatField(Text.translatable("config_value.squake.softCapDegen"), configValues.softCapDegen)
                .setDefaultValue(0.65F)
                .setMin(0)
                .setMax(Float.MAX_VALUE)
                .setTooltip(Text.translatable("tooltip.squake.softCapDegen"))
                .setSaveConsumer(newValue -> configValues.softCapDegen = newValue)
                .build());
        category.addEntry(entryBuilder.startFloatField(Text.translatable("config_value.squake.increasedFallDistance"), configValues.increasedFallDistance)
                .setDefaultValue(0F)
                .setMin(0)
                .setMax(Float.MAX_VALUE)
                .setTooltip(Text.translatable("tooltip.squake.increasedFallDistance"))
                .setSaveConsumer(newValue -> configValues.increasedFallDistance = newValue)
                .build());

        builder.setSavingRunnable(ConfigHandler::save);
        this.configScreen = builder.build();
    }

    public Screen getScreen() {
        return this.configScreen;
    }
}
