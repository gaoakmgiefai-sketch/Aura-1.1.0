package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.setting.NumberSetting;
import net.minecraft.text.Text;

public class AutoLeave extends Module {
    private final NumberSetting health = add(new NumberSetting("Leave at HP", 4, 1, 19, 1));

    public AutoLeave() {
        super("Auto Leave", "Disconnects when health is low", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (mc.player.getHealth() <= health.floatValue() && mc.player.isAlive()) {
            setEnabledQuiet(false);
            mc.getNetworkHandler().getConnection().disconnect(Text.literal("[Aura] Auto Leave: low health"));
        }
    }
}
