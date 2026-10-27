package com.example.aura.module;

import com.example.aura.AuraMod;
import com.example.aura.modules.*;
import com.example.aura.util.Keys;
import net.minecraft.client.MinecraftClient;

import java.util.*;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static final EnumMap<Category, List<Module>> BY_CAT = new EnumMap<>(Category.class);

    private ModuleManager() {}

    public static void init() {
        clear();
        // Combat
        add(new KillAura());
        add(new TriggerBot());
        add(new Criticals());
        add(new AntiBot());
        add(new AutoTotem());
        // Movement
        add(new Sprint());
        add(new Flight());
        add(new AirJump());
        add(new AutoJump());
        add(new NoFall());
        add(new GuiMove());
        // Visuals
        add(new Fullbright());
        add(new ESP());
        add(new TargetESP());
        add(new ArmorHud());
        add(new HudModule());
        // Player
        add(new AutoRespawn());
        add(new AutoEat());
        add(new AutoArmor());
        add(new AutoTool());
        add(new ClickPearl());
        add(new AntiAFK());
        // Misc
        add(new AutoLeave());
    }

    private static void add(Module m) {
        MODULES.add(m);
        BY_CAT.computeIfAbsent(m.category, c -> new ArrayList<>()).add(m);
    }

    public static void clear() {
        MODULES.clear();
        BY_CAT.clear();
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(MODULES);
    }

    public static List<Module> byCategory(Category category) {
        return BY_CAT.getOrDefault(category, Collections.emptyList());
    }

    public static <T extends Module> T get(Class<T> type) {
        for (Module m : MODULES) if (type.isInstance(m)) return type.cast(m);
        throw new IllegalStateException("Module not registered: " + type.getSimpleName());
    }

    public static void tickPre(MinecraftClient mc) {
        handleKeys(mc);
        for (int i = 0; i < MODULES.size(); i++) {
            Module m = MODULES.get(i);
            if (m.isEnabled()) run(m, true);
        }
    }

    public static void tickPost() {
        for (int i = 0; i < MODULES.size(); i++) {
            Module m = MODULES.get(i);
            if (m.isEnabled()) run(m, false);
        }
    }

    private static void run(Module m, boolean pre) {
        try {
            if (pre) m.onPreTick(); else m.onTick();
        } catch (Throwable t) {
            AuraMod.LOGGER.error("Module {} failed on {}", m.name, pre ? "preTick" : "tick", t);
            m.setEnabledQuiet(false);
        }
    }

    private static void handleKeys(MinecraftClient mc) {
        boolean canToggle = mc.currentScreen == null;
        for (int i = 0; i < MODULES.size(); i++) {
            Module m = MODULES.get(i);
            if (m.key < 0) continue;
            boolean down = Keys.key(m.key);
            if (down && !m.keyDown && canToggle) m.toggle();
            m.keyDown = down;
        }
    }
}
