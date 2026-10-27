package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.setting.BoolSetting;
import com.example.aura.setting.NumberSetting;
import com.example.aura.util.Keys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class AutoEat extends Module {
    private final NumberSetting hunger = add(new NumberSetting("Eat at hunger", 14, 1, 19, 1));
    private final BoolSetting gapple = add(new BoolSetting("Golden apple", true));
    private final NumberSetting gappleHp = add(new NumberSetting("Gapple at HP", 10, 2, 20, 1), gapple::get);

    private boolean eating;
    private int prevSlot = -1;

    public AutoEat() {
        super("Auto Eat", "Eats food (and golden apples) automatically", Category.PLAYER);
    }

    private boolean edible(ItemStack s) {
        if (s.isEmpty() || s.get(DataComponentTypes.FOOD) == null) return false;
        return !(s.isOf(Items.ROTTEN_FLESH) || s.isOf(Items.SPIDER_EYE) || s.isOf(Items.PUFFERFISH)
                || s.isOf(Items.POISONOUS_POTATO) || s.isOf(Items.CHORUS_FRUIT) || s.isOf(Items.SUSPICIOUS_STEW));
    }

    private int find(ClientPlayerEntity p, boolean wantGapple) {
        int best = -1, bestN = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (!edible(s)) continue;
            boolean isG = s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE);
            if (wantGapple != isG) continue;
            FoodComponent f = s.get(DataComponentTypes.FOOD);
            if (f.nutrition() > bestN) { bestN = f.nutrition(); best = i; }
        }
        return best;
    }

    @Override
    public void onPreTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) return;

        boolean needGapple = gapple.get() && p.getHealth() <= gappleHp.floatValue()
                && !p.hasStatusEffect(StatusEffects.REGENERATION);
        boolean hungry = p.getHungerManager().getFoodLevel() <= hunger.intValue();

        if (eating) {
            if (!p.isUsingItem() && !(hungry || needGapple)) { stop(p); return; }
            mc.options.useKey.setPressed(true);
            return;
        }
        if (mc.currentScreen != null || p.isUsingItem()) return;

        int slot = needGapple ? find(p, true) : -1;
        if (slot < 0 && hungry) slot = find(p, false);
        if (slot < 0) return;

        prevSlot = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(slot);
        eating = true;
    }

    private void stop(ClientPlayerEntity p) {
        mc.options.useKey.setPressed(Keys.physical(mc.options.useKey));
        if (prevSlot >= 0) p.getInventory().setSelectedSlot(prevSlot);
        prevSlot = -1;
        eating = false;
    }

    @Override
    public void onDisable() {
        if (mc.player != null && eating) stop(mc.player);
        eating = false;
    }
}
