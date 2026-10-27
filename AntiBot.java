package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/** Исключает из целей "игроков", которых нет в таб-листе (NPC / фейки). */
public class AntiBot extends Module {
    private static AntiBot instance;
    private final NumberSetting grace = add(new NumberSetting("Spawn grace (ticks)", 0, 0, 40, 1));

    public AntiBot() {
        super("Anti Bot", "Ignore fake players missing from the tab list", Category.COMBAT);
        instance = this;
    }

    public static boolean isBot(Entity e) {
        AntiBot ab = instance;
        if (ab == null || !ab.isEnabled() || !(e instanceof PlayerEntity p)) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null || p == mc.player) return false;
        if (mc.getNetworkHandler().getPlayerListEntry(p.getUuid()) == null) return true;
        return p.age < ab.grace.intValue();
    }
}
