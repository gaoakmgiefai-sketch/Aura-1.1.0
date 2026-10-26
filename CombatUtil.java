package com.example.aura.util;

import com.example.aura.module.ModuleManager;
import com.example.aura.modules.Criticals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;

public final class CombatUtil {
    private CombatUtil() {}

    /**
     * Обычная атака. Если нужен сброс спринта (для критов), сервер получает
     * STOP_SPRINTING -> удар -> START_SPRINTING, как при настоящем sprint-reset.
     */
    public static void attack(MinecraftClient mc, Entity target) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || mc.getNetworkHandler() == null) return;

        boolean reset = ModuleManager.get(Criticals.class).wantsSprintReset(p);
        if (reset) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(p, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
        mc.interactionManager.attackEntity(p, target);
        p.swingHand(Hand.MAIN_HAND);
        if (reset) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(p, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
    }
}
