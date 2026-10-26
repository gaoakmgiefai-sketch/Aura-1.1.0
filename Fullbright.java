package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/** Клиентский эффект ночного зрения — работает без миксинов и на любой версии. */
public class Fullbright extends Module {
    public Fullbright() {
        super("Fullbright", "Always bright (client-side night vision)", Category.VISUAL);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        StatusEffectInstance current = p.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (current == null || current.getDuration() < 300) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1200, 0, false, false, false));
        }
    }

    @Override
    public void onDisable() {
        ClientPlayerEntity p = mc.player;
        if (p != null) p.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
