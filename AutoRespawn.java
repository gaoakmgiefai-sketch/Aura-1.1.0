package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;

public class AutoRespawn extends Module {
    public AutoRespawn() {
        super("Auto Respawn", "Respawns instantly after death", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player != null && mc.player.isDead()) mc.player.requestRespawn();
    }
}
