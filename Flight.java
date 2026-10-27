package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.setting.ModeSetting;
import com.example.aura.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;

public class Flight extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", 0, "Vanilla", "Glide"));
    private final NumberSetting speed = add(new NumberSetting("Speed", 0.6, 0.1, 2.0, 0.05));
    private final NumberSetting vSpeed = add(new NumberSetting("Vertical", 0.4, 0.1, 1.5, 0.05));

    public Flight() {
        super("Flight", "Fly freely", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.currentScreen != null) return;

        double f = (mc.options.forwardKey.isPressed() ? 1 : 0) - (mc.options.backKey.isPressed() ? 1 : 0);
        double s = (mc.options.leftKey.isPressed() ? 1 : 0) - (mc.options.rightKey.isPressed() ? 1 : 0);
        double len = Math.sqrt(f * f + s * s);
        if (len > 1e-4) { f /= len; s /= len; }
        double rad = Math.toRadians(p.getYaw());
        double sin = Math.sin(rad), cos = Math.cos(rad);
        double hs = speed.get();
        double vx = (-sin * f + cos * s) * hs;
        double vz = (cos * f + sin * s) * hs;

        double vy = mode.get().equals("Glide") ? -0.03 : 0.0;
        if (mc.options.jumpKey.isPressed()) vy = vSpeed.get();
        else if (mc.options.sneakKey.isPressed()) vy = -vSpeed.get();

        p.setVelocity(vx, vy, vz);
        p.fallDistance = 0;
    }
}
