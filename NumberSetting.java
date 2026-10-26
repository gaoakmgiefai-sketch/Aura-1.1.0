package com.example.aura.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public class NumberSetting extends Setting<Double> {
    public final double min;
    public final double max;
    public final double step;

    public NumberSetting(String name, double def, double min, double max, double step) {
        super(name, def);
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = snap(def);
    }

    private double snap(double v) {
        v = Math.max(min, Math.min(max, v));
        v = Math.round(v / step) * step;
        v = Math.round(v * 10000.0) / 10000.0;
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public void set(Double v) {
        value = snap(v);
    }

    public int intValue() {
        return (int) Math.round(value);
    }

    public float floatValue() {
        return value.floatValue();
    }

    public String display() {
        if (step >= 1) return Integer.toString(intValue());
        return String.format(Locale.ROOT, step < 0.1 ? "%.2f" : "%.1f", value);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement element) {
        set(element.getAsDouble());
    }
}
