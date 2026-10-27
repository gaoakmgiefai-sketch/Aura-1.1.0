package com.example.aura.util;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Физическое состояние клавиш (независимо от открытых экранов). */
public final class Keys {
    private Keys() {}

    public static boolean physical(KeyBinding kb) {
        InputUtil.Key k = KeyBindingHelper.getBoundKeyOf(kb);
        if (k.getCode() == GLFW.GLFW_KEY_UNKNOWN) return false;
        long h = MinecraftClient.getInstance().getWindow().getHandle();
        if (k.getCategory() == InputUtil.Type.MOUSE) return GLFW.glfwGetMouseButton(h, k.getCode()) == GLFW.GLFW_PRESS;
        return InputUtil.isKeyPressed(h, k.getCode());
    }

    public static boolean key(int code) {
        if (code < 0) return false;
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), code);
    }

    public static boolean mouse(int button) {
        return GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), button) == GLFW.GLFW_PRESS;
    }
}
