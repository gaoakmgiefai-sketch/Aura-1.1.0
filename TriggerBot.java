package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.module.ModuleManager;
import com.example.aura.setting.NumberSetting;
import com.example.aura.util.CombatUtil;
import com.example.aura.util.TargetSettings;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

import java.util.concurrent.ThreadLocalRandom;

/** Бьёт сущность, на которую ты сам навёл прицел. Никаких ротаций — максимально «чисто». */
public class TriggerBot extends Module {
    private final NumberSetting jitter = add(new NumberSetting("Click jitter (ticks)", 1, 0, 3, 1));
    private final TargetSettings targets = new TargetSettings();

    private int delay;
    private float needCooldown = 0.95f;

    public TriggerBot() {
        super("TriggerBot", "Attacks the entity under your crosshair", Category.COMBAT);
        targets.addTo(this);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.currentScreen != null || !p.isAlive() || p.isUsingItem()) return;
        if (delay > 0) {
            delay--;
            return;
        }
        Entity e = mc.targetedEntity;
        if (e == null || !targets.matches(e, p)) return;
        if (p.getAttackCooldownProgress(0.5f) < needCooldown) return;
        if (!ModuleManager.get(Criticals.class).allowAttack(e)) return;

        CombatUtil.attack(mc, e);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        delay = jitter.intValue() > 0 ? rnd.nextInt(jitter.intValue() + 1) : 0;
        needCooldown = 0.92f + rnd.nextFloat() * 0.08f;
    }
}
