package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotem extends Module {
    private final NumberSetting health = add(new NumberSetting("Health", 20, 1, 20, 1));
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 2, 0, 10, 1));
    private int cooldown;

    public AutoTotem() {
        super("Auto Totem", "Keeps a totem in your offhand", Category.COMBAT);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (cooldown > 0) { cooldown--; return; }
        if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        if (p.getHealth() + p.getAbsorptionAmount() > health.floatValue()) return;

        int slot = -1;
        for (int i = 9; i < 36 && slot < 0; i++) if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) slot = i;
        for (int i = 0; i < 9 && slot < 0; i++) if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) slot = 36 + i;
        if (slot < 0) return;

        mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, p);
        cooldown = delay.intValue();
    }
}
