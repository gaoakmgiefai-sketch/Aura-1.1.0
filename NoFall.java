package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall extends Module {
    private final NumberSetting distance = add(new NumberSetting("Min distance", 2.5, 1.0, 10.0, 0.5));

    public NoFall() {
        super("No Fall", "Cancels fall damage", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.getNetworkHandler() == null) return;
        if (p.fallDistance > distance.get() && !p.isGliding() && !p.isOnGround()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, p.horizontalCollision));
            p.fallDistance = 0;
        }
    }
}
