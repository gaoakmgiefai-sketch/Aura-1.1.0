package com.example.aura.modules;

import com.example.aura.gui.AuraMenu;
import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.util.Keys;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.option.KeyBinding;

public class GuiMove extends Module {
    public GuiMove() {
        super("Gui Move", "Move while inventories are open", Category.MOVEMENT);
    }

    @Override
    public void onPreTick() {
        Screen s = mc.currentScreen;
        if (s == null || mc.player == null) return;
        if (s instanceof ChatScreen || s instanceof AuraMenu || s instanceof AbstractSignEditScreen || s instanceof BookEditScreen) return;
        KeyBinding[] keys = { mc.options.forwardKey, mc.options.backKey, mc.options.leftKey,
                mc.options.rightKey, mc.options.jumpKey, mc.options.sprintKey };
        for (KeyBinding k : keys) k.setPressed(Keys.physical(k));
    }
}
