package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.util.Keys;
import net.minecraft.client.network.ClientPlayerEntity;

public class AutoJump extends Module {
    private boolean forced;

    public AutoJump() {
        super("Auto Jump", "Jumps automatically while moving", Category.MOVEMENT);
    }

    @Override
    public void onPreTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.currentScreen != null) return;
        boolean moving = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
        if (moving && p.isOnGround() && !p.isSneaking()) {
            mc.options.jumpKey.setPressed(true);
            forced = true;
        } else if (forced) {
            mc.options.jumpKey.setPressed(Keys.physical(mc.options.jumpKey));
            forced = false;
        }
    }

    @Override
    public void onDisable() {
        if (forced) mc.options.jumpKey.setPressed(Keys.physical(mc.options.jumpKey));
        forced = false;
    }
}
