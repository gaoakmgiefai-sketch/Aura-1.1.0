package com.example.aura.modules;

import com.example.aura.gui.MenuStyle;
import com.example.aura.module.Category;
import com.example.aura.module.Module;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class ArmorHud extends Module {
    private static final EquipmentSlot[] ORDER = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    public ArmorHud() {
        super("Armor Durability", "Shows armor durability on screen", Category.VISUAL);
        HudRenderCallback.EVENT.register(this::render);
    }

    private void render(DrawContext g, RenderTickCounter tc) {
        if (!isEnabled() || mc.player == null || mc.options.hudHidden) return;
        int x = mc.getWindow().getScaledWidth() - 52;
        int y = mc.getWindow().getScaledHeight() / 2 - 34;
        for (EquipmentSlot slot : ORDER) {
            ItemStack st = mc.player.getEquippedStack(slot);
            if (st.isEmpty()) continue;
            g.drawItem(st, x, y);
            if (st.isDamageable()) {
                float frac = 1f - st.getDamage() / (float) st.getMaxDamage();
                int color = MenuStyle.lerp(0xFFFF5555, 0xFF55FF88, frac);
                g.drawText(mc.textRenderer, Math.round(frac * 100) + "%", x + 19, y + 4, color, true);
            }
            y += 18;
        }
    }
}
