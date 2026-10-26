package com.example.aura.gui;

import net.minecraft.client.gui.DrawContext;

public final class Theme {
    public static final int BG = 0xFF121216;
    public static final int OUTLINE = 0xFF2A2A36;
    public static final int PANEL = 0xFF1A1A21;
    public static final int PANEL_HOV = 0xFF21212B;
    public static final int PANEL_SEL = 0xFF292936;
    public static final int TRACK = 0xFF2E2E3B;
    public static final int ACCENT = 0xFF8B5CFF;
    public static final int ACCENT2 = 0xFF3DD6F5;
    public static final int TEXT = 0xFFEDEDF5;
    public static final int MUTED = 0xFF8C8CA0;

    private Theme() {}

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 255, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int oa = Math.round(aa + (ba - aa) * t);
        int or = Math.round(ar + (br - ar) * t);
        int og = Math.round(ag + (bg - ag) * t);
        int ob = Math.round(ab + (bb - ab) * t);
        return (oa << 24) | (or << 16) | (og << 8) | ob;
    }

    public static float approach(float cur, float target, float dt, float speed) {
        return cur + (target - cur) * (1f - (float) Math.exp(-speed * dt));
    }

    /** Прямоугольник со скруглёнными (радиус ~2px) углами. */
    public static void rrect(DrawContext c, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        if (w < 6 || h < 6) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }
        c.fill(x + 2, y, x + w - 2, y + h, color);
        c.fill(x, y + 2, x + 2, y + h - 2, color);
        c.fill(x + w - 2, y + 2, x + w, y + h - 2, color);
        c.fill(x + 1, y + 1, x + 2, y + 2, color);
        c.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
        c.fill(x + 1, y + h - 2, x + 2, y + h - 1, color);
        c.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
    }

    public static void gradientH(DrawContext c, int x, int y, int w, int h, int left, int right) {
        int seg = Math.max(1, Math.min(w, 64));
        for (int i = 0; i < seg; i++) {
            int x1 = x + w * i / seg;
            int x2 = x + w * (i + 1) / seg;
            c.fill(x1, y, x2, y + h, lerp(left, right, (i + 0.5f) / seg));
        }
    }

    public static void toggle(DrawContext c, int x, int y, float a) {
        rrect(c, x, y, 22, 10, lerp(0xFF3A3A48, ACCENT, a));
        rrect(c, x + 1 + Math.round(a * 12), y + 1, 8, 8, 0xFFFFFFFF);
    }
}
