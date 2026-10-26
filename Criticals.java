package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.setting.BoolSetting;
import com.example.aura.setting.ModeSetting;
import com.example.aura.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/**
 * Новый режим критов. Работает вместе с KillAura и TriggerBot:
 * они спрашивают {@link #allowAttack(Entity)} перед каждым ударом.
 *
 * Fall / MiniJump — «честные» криты: удар откладывается, пока игрок падает.
 * Packet / Packet Strict — серверу отправляются пакеты мини-прыжка прямо перед ударом.
 */
public class Criticals extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", 0, "Fall", "MiniJump", "Packet", "Packet Strict"));
    private final BoolSetting autoJump = add(new BoolSetting("Auto jump", true), this::isWaitMode);
    private final NumberSetting maxWait = add(new NumberSetting("Max wait (ticks)", 14, 4, 30, 1), this::isWaitMode);
    private final BoolSetting sprintReset = add(new BoolSetting("Sprint reset", true));

    private int waiting;

    public Criticals() {
        super("Criticals", "Critical hits: wait for fall or packet based", Category.COMBAT);
    }

    private boolean isWaitMode() {
        return mode.is("Fall") || mode.is("MiniJump");
    }

    @Override
    public String getSuffix() {
        return mode.get();
    }

    @Override
    public void onDisable() {
        waiting = 0;
    }

    /** Можно ли сейчас вообще получить крит по правилам ванильного сервера. */
    public boolean canCrit(ClientPlayerEntity p) {
        return !p.hasVehicle()
                && !p.isClimbing()
                && !p.isTouchingWater()
                && !p.isInLava()
                && !p.isSpectator()
                && !p.getAbilities().flying
                && !p.hasStatusEffect(StatusEffects.BLINDNESS);
    }

    private boolean isFalling(ClientPlayerEntity p) {
        return !p.isOnGround() && p.getVelocity().y < 0 && p.fallDistance > 0;
    }

    /** Нужно ли «сбросить» спринт на сервере (в ванили критовать в спринте нельзя). */
    public boolean wantsSprintReset(ClientPlayerEntity p) {
        return isEnabled() && sprintReset.get() && p.isSprinting() && canCrit(p);
    }

    /**
     * @return true — удар можно наносить прямо сейчас; false — подождать (прыгаем/падаем).
     * В режимах Packet пакеты отправляются внутри этого метода.
     */
    public boolean allowAttack(Entity target) {
        if (!isEnabled()) return true;
        ClientPlayerEntity p = mc.player;
        if (p == null || !canCrit(p)) {
            waiting = 0;
            return true;
        }

        if (isWaitMode()) {
            if (isFalling(p)) {
                waiting = 0;
                return true;
            }
            // Не держим удар вечно (потолок, паутина и т.п.).
            if (++waiting > maxWait.intValue()) {
                waiting = 0;
                return true;
            }
            if (p.isOnGround() && autoJump.get()) {
                double jump = mode.is("MiniJump") ? 0.2 : 0.42;
                p.setVelocity(p.getVelocity().x, jump, p.getVelocity().z);
            }
            return false;
        }

        // Packet-режимы: имитируем маленький прыжок для сервера.
        if (p.isOnGround() && mc.getNetworkHandler() != null) {
            double[] offsets = mode.is("Packet Strict")
                    ? new double[]{0.11, 0.1100013579, 0.0000013579}
                    : new double[]{0.0625, 0.0};
            for (double o : offsets) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        p.getX(), p.getY() + o, p.getZ(), false, p.horizontalCollision));
            }
        }
        return true;
    }
}
