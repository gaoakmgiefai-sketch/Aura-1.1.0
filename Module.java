package com.example.aura.module;

import com.example.aura.AuraMod;
import com.example.aura.setting.Setting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;
    public final List<Setting<?>> settings = new ArrayList<>();
    protected final MinecraftClient mc = MinecraftClient.getInstance();

    /** GLFW-код клавиши или -1. */
    public int key = GLFW.GLFW_KEY_UNKNOWN;
    /** Анимация переключателя для GUI. */
    public float anim;
    boolean keyDown;
    private boolean enabled;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    public <S extends Setting<?>> S add(S setting, BooleanSupplier visible) {
        setting.setVisible(visible);
        settings.add(setting);
        return setting;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        try {
            if (value) onEnable();
            else onDisable();
        } catch (Throwable t) {
            AuraMod.LOGGER.error("Module {} failed on {}", name, value ? "enable" : "disable", t);
        }
    }

    /** Без вызова onEnable/onDisable — для загрузки конфига и значений по умолчанию. */
    public void setEnabledQuiet(boolean value) {
        enabled = value;
    }

    public void onEnable() {}

    public void onDisable() {}

    /** Начало тика (до отправки пакетов движения). Игрок и мир гарантированно не null. */
    public void onPreTick() {}

    /** Конец тика (после пакетов движения). Игрок и мир гарантированно не null. */
    public void onTick() {}

    /** Короткая подпись справа от названия в списке модулей на HUD. */
    public String getSuffix() {
        return null;
    }
}
