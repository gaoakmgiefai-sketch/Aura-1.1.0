package com.example.aura.module;

import com.example.aura.AuraMod;
import com.example.aura.modules.Criticals;
import com.example.aura.modules.Fullbright;
import com.example.aura.modules.HudModule;
import com.example.aura.modules.KillAura;
import com.example.aura.modules.Sprint;
import com.example.aura.modules.TriggerBot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() {}

    public static void init() {
        MODULES.clear();
        MODULES.add(new KillAura());
        MODULES.add(new Criticals());
        MODULES.add(new TriggerBot());
        MODULES.add(new Sprint());
        MODULES.add(new Fullbright());
        MODULES.add(new HudModule());
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(MODULES);
    }

    public static List<Module> byCategory(Category category) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) if (m.category == category) out.add(m);
        return out;
    }

    public static <T extends Module> T get(Class<T> type) {
        for (Module m : MODULES) if (type.isInstance(m)) return type.cast(m);
        throw new IllegalStateException("Module not registered: " + type.getSimpleName());
    }

    public static void tickPre(MinecraftClient mc) {
        handleKeys(mc);
        for (Module m : MODULES) if (m.isEnabled()) run(m, true);
    }

    public static void tickPost() {
        for (Module m : MODULES) if (m.isEnabled()) run(m, false);
    }

    private static void run(Module m, boolean pre) {
        try {
            if (pre) m.onPreTick();
            else m.onTick();
        } catch (Throwable t) {
            // Один сломанный модуль не должен ронять игру — отключаем и пишем в лог.
            AuraMod.LOGGER.error("Module {} crashed and was disabled", m.name, t);
            m.setEnabled(false);
        }
    }

    private static void handleKeys(MinecraftClient mc) {
        if (mc.currentScreen != null) return;
        long handle = mc.getWindow().getHandle();
        for (Module m : MODULES) {
            if (m.key < 0) continue;
            boolean down = InputUtil.isKeyPressed(handle, m.key);
            if (down && !m.keyDown) m.toggle();
            m.keyDown = down;
        }
    }
}
