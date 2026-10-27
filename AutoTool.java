package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/** Выбирает самый быстрый инструмент в хотбаре при копании и возвращает слот после. */
public class AutoTool extends Module {
    private int prevSlot = -1;

    public AutoTool() {
        super("Auto Tool", "Picks the best tool while mining", Category.PLAYER);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;

        boolean mining = mc.options.attackKey.isPressed() && mc.currentScreen == null
                && mc.crosshairTarget instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK;
        if (!mining) { restore(p); return; }

        BlockState state = mc.world.getBlockState(((BlockHitResult) mc.crosshairTarget).getBlockPos());
        int cur = p.getInventory().getSelectedSlot();
        int best = cur;
        float bestSpeed = p.getInventory().getStack(cur).getMiningSpeedMultiplier(state);
        for (int i = 0; i < 9; i++) {
            float s = p.getInventory().getStack(i).getMiningSpeedMultiplier(state);
            if (s > bestSpeed + 0.01f) { bestSpeed = s; best = i; }
        }
        if (best != cur) {
            if (prevSlot < 0) prevSlot = cur;
            p.getInventory().setSelectedSlot(best);
        }
    }

    private void restore(ClientPlayerEntity p) {
        if (prevSlot >= 0) { p.getInventory().setSelectedSlot(prevSlot); prevSlot = -1; }
    }

    @Override
    public void onDisable() {
        if (mc.player != null) restore(mc.player);
        prevSlot = -1;
    }
}
