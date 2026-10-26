package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import net.minecraft.client.network.ClientPlayerEntity;

public class Sprint extends Module {
    public Sprint() {
        super("Sprint", "Automatically sprints while moving forward", Category.MOVEMENT);
    }

    @Override
    public void onPreTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || p.isSprinting()) return;
        if (!mc.options.forwardKey.isPressed()) return;
        if (p.horizontalCollision || p.isSneaking() || p.isUsingItem() || p.hasVehicle()) return;
        if (p.getHungerManager().getFoodLevel() <= 6 && !p.getAbilities().allowFlying) return;
        p.setSprinting(true);
    }
}
