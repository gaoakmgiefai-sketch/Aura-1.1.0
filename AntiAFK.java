package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.setting.NumberSetting;
import com.example.aura.util.Keys;
import net.minecraft.util.Hand;

public class AntiAFK extends Module {
    private final NumberSetting interval = add(new NumberSetting("Interval (sec)", 20, 5, 120, 5));
    private int timer, jumpTicks;

    public AntiAFK() {
        super("Anti AFK", "Prevents AFK kick", Category.PLAYER);
    }

    @Override
    public void onPreTick() {
        if (mc.player == null || mc.currentScreen != null) return;
        if (jumpTicks > 0 && --jumpTicks == 0) mc.options.jumpKey.setPressed(Keys.physical(mc.options.jumpKey));
        if (++timer >= interval.intValue() * 20) {
            timer = 0;
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.options.jumpKey.setPressed(true);
            jumpTicks = 2;
        }
    }

    @Override
    public void onDisable() {
        timer = 0;
        if (jumpTicks > 0) mc.options.jumpKey.setPressed(Keys.physical(mc.options.jumpKey));
        jumpTicks = 0;
    }
}
