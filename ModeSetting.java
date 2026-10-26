package com.example.aura.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ModeSetting extends Setting<String> {
    public final String[] modes;
    private int index;

    public ModeSetting(String name, int def, String... modes) {
        super(name, modes[def]);
        this.modes = modes;
        this.index = def;
    }

    @Override
    public void set(String v) {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equalsIgnoreCase(v)) {
                index = i;
                value = modes[i];
                return;
            }
        }
    }

    public void cycle(int dir) {
        index = Math.floorMod(index + dir, modes.length);
        value = modes[index];
    }

    public boolean is(String mode) {
        return value.equals(mode);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement element) {
        set(element.getAsString());
    }
}
