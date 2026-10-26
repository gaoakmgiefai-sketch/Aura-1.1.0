package com.example.aura.config;

import com.example.aura.AuraMod;
import com.example.aura.module.Module;
import com.example.aura.module.ModuleManager;
import com.example.aura.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigManager() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("aura.json");
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (Module m : ModuleManager.all()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("enabled", m.isEnabled());
                obj.addProperty("key", m.key);
                JsonObject s = new JsonObject();
                for (Setting<?> setting : m.settings) s.add(setting.name, setting.toJson());
                obj.add("settings", s);
                root.add(m.name, obj);
            }
            Files.writeString(file(), GSON.toJson(root));
        } catch (Exception e) {
            AuraMod.LOGGER.error("Failed to save config", e);
        }
    }

    public static void load() {
        Path path = file();
        if (!Files.exists(path)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            for (Module m : ModuleManager.all()) {
                if (!root.has(m.name)) continue;
                JsonObject obj = root.getAsJsonObject(m.name);
                if (obj.has("enabled")) m.setEnabledQuiet(obj.get("enabled").getAsBoolean());
                if (obj.has("key")) m.key = obj.get("key").getAsInt();
                if (!obj.has("settings")) continue;
                JsonObject s = obj.getAsJsonObject("settings");
                for (Setting<?> setting : m.settings) {
                    JsonElement el = s.get(setting.name);
                    if (el == null) continue;
                    try {
                        setting.fromJson(el);
                    } catch (Exception ignored) {
                        // битое значение — оставляем по умолчанию
                    }
                }
            }
        } catch (Exception e) {
            AuraMod.LOGGER.error("Failed to load config, using defaults", e);
        }
    }
}
