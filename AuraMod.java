package com.example.aura;

import com.example.aura.config.ConfigManager;
import com.example.aura.gui.ClickGui;
import com.example.aura.modules.HudModule;
import com.example.aura.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuraMod implements ClientModInitializer {
    public static final String MOD_ID = "auramod";
    public static final Logger LOGGER = LoggerFactory.getLogger("Aura");
    public static String VERSION = "dev";
    public static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        VERSION = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");

        ModuleManager.init();
        ConfigManager.load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aura.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.aura.mod"));

        // Начало тика: ротации и подготовка (до пакетов движения).
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            ModuleManager.tickPre(client);
        });

        // Конец тика: атаки (после пакетов движения, поэтому сервер уже знает новую ротацию).
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGui());
            }
            if (client.player == null || client.world == null) return;
            ModuleManager.tickPost();
        });

        HudRenderCallback.EVENT.register((context, tickCounter) ->
                ModuleManager.get(HudModule.class).render(context));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());
        LOGGER.info("Aura {} loaded", VERSION);
    }
}
