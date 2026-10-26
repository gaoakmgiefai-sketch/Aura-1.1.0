package com.example.aura.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    public final String name;
    protected T value;
    /** Состояние анимации для GUI. */
    public float anim;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public void setVisible(BooleanSupplier visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible.getAsBoolean();
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement element);
}
