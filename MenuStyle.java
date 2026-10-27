package com.example.aura.gui;

import net.minecraft.client.gui.DrawContext;

/** Цвета и мелкие рисовалки для меню. Без аллокаций в рендере. */
public final class MenuStyle {
    private MenuStyle() {}

    public static final int PANEL      = 0xC8101424;
    public static final int HEADER     = 0xE0161B30;
    public static final int ROW_HOVER  = 0x30FFFFFF;
    public static final int ROW_ON     = 0x405B7CFF;
    public static final int ACCENT     = 0xFF6C8CFF;
    public static final int ACCENT2    = 0xFFB07CFF;
    public static final int TEXT       = 0xFFE8ECFF;
    public static final int MUTED      = 0xFF8D96B8;
    public static final int TRACK      = 0xFF262C47;

    /** Экспоненциальное сглаживание, не зависит от FPS. */
    public static float approach(float cur, float target, float speed, float dt) {
        return cur + (target - cur) * (1f - (float) Math.exp(-speed * dt));
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
             | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static int alpha(int color, float mul) {
        int a = (int) ((color >>> 24) * Math.max(0f, Math.min(1f, mul)));
        return (a << 24) | (color & 0xFFFFFF);
    }

    /** Скруглённый прямоугольник: центр + построчные срезанные углы. */
    public static void rrect(DrawContext g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int d = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            g.fill(x + d, y + i, x + w - d, y + i + 1, color);
            g.fill(x + d, y + h - i - 1, x + w - d, y + h - i, color);
        }
    }

    public static void gradientH(DrawContext g, int x, int y, int w, int h, int c1, int c2) {
        int seg = Math.max(1, Math.min(w, 24));
        float sw = (float) w / seg;
        for (int i = 0; i < seg; i++) {
            int x1 = x + Math.round(i * sw), x2 = x + Math.round((i + 1) * sw);
            g.fill(x1, y, x2, y + h, lerp(c1, c2, i / (float) Math.max(1, seg - 1)));
        }
    }
}
