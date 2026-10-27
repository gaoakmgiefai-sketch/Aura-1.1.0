package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.util.Keys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/** Средняя кнопка мыши - бросок жемчуга из хотбара и возврат на прежний слот. */
public class ClickPearl extends Module {
    private boolean was;

    public ClickPearl() {
        super("Click Pearl", "Middle click throws an ender pearl", Category.PLAYER);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) return;
        boolean down = mc.currentScreen == null && Keys.mouse(2);
        if (down && !was) throwPearl(p);
        was = down;
    }

    private void throwPearl(ClientPlayerEntity p) {
        int slot = -1;
        for (int i = 0; i < 9; i++) if (p.getInventory().getStack(i).isOf(Items.ENDER_PEARL)) { slot = i; break; }
        if (slot < 0) return;
        int prev = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(slot);
        mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
        p.swingHand(Hand.MAIN_HAND);
        p.getInventory().setSelectedSlot(prev);
    }
}
