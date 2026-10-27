package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/** Надевает броню из инвентаря в пустые слоты брони. */
public class AutoArmor extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 3, 0, 20, 1));
    private int cooldown;

    public AutoArmor() {
        super("Auto Armor", "Equips armor into empty armor slots", Category.PLAYER);
    }

    private static int armorSlotId(EquipmentSlot s) {
        return switch (s) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> -1;
        };
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (cooldown > 0) { cooldown--; return; }

        for (int i = 0; i < 36; i++) {
            ItemStack st = p.getInventory().getStack(i);
            if (st.isEmpty() || st.isOf(Items.ELYTRA)) continue;
            EquippableComponent eq = st.get(DataComponentTypes.EQUIPPABLE);
            if (eq == null || !eq.slot().isArmorSlot()) continue;
            int target = armorSlotId(eq.slot());
            if (target < 0 || !p.getEquippedStack(eq.slot()).isEmpty()) continue;

            int from = i < 9 ? 36 + i : i;
            mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, from, 0, SlotActionType.QUICK_MOVE, p);
            cooldown = delay.intValue();
            return;
        }
    }
}
