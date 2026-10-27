package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class AirJump extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 4, 0, 20, 1));
    private boolean was;
    private int cd;

    public AirJump() {
        super("Air Jump", "Jump again in the air", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        boolean j = mc.options.jumpKey.isPressed();
        if (cd > 0) cd--;
        if (j && !was && cd == 0 && mc.currentScreen == null && !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater()) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, 0.42, v.z);
            cd = delay.intValue();
        }
        was = j;
    }
}
