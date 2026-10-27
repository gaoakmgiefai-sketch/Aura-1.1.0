package com.example.aura.gui;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.module.ModuleManager;
import com.example.aura.setting.BoolSetting;
import com.example.aura.setting.ModeSetting;
import com.example.aura.setting.NumberSetting;
import com.example.aura.setting.Setting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.*;

/**
 * Меню в 5 колонок: Combat | Movement | Visuals | Player | Miscellaneous.
 * ЛКМ - вкл/выкл модуль, ПКМ - раскрыть настройки, колесо - скролл колонки,
 * набор текста - поиск по модулям.
 */
public class AuraMenu extends Screen {
    private static final int COL_W = 98, GAP = 8, HEAD_H = 20, ROW_H = 15, SET_H = 14, SLIDER_H = 20, PAD = 4;

    private record Hit(int x, int y, int w, int h, Module module, Setting<?> setting) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Hit> hits = new ArrayList<>(64);
    private final Map<Module, Float> hover = new IdentityHashMap<>();
    private final Map<Module, Float> openAnim = new IdentityHashMap<>();
    private final Set<Module> expanded = Collections.newSetFromMap(new IdentityHashMap<>());
    private final EnumMap<Category, Float> scroll = new EnumMap<>(Category.class);

    private String query = "";
    private float appear;
    private long lastNs = System.nanoTime();
    private Hit dragging;
    private Module binding;
    private final Map<Integer, String> keyNames = new HashMap<>();

    public AuraMenu() {
        super(Text.literal("Aura"));
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        super.render(g, mx, my, delta); // размытый фон
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        appear = MenuStyle.approach(appear, 1f, 14f, dt);

        TextRenderer tr = textRenderer;
        Category[] cats = Category.values();
        int totalW = cats.length * COL_W + (cats.length - 1) * GAP;
        int left = (width - totalW) / 2;
        int top = Math.max(16, (int) (height * 0.17f)) + (int) ((1f - appear) * 14);
        int maxH = height - top - 44;
        float a = appear;

        hits.clear();
        if (dragging != null && dragging.setting() instanceof NumberSetting ns) applySlider(ns, dragging, mx);

        for (int ci = 0; ci < cats.length; ci++) {
            Category cat = cats[ci];
            int x = left + ci * (COL_W + GAP);

            List<Module> mods = filtered(cat);
            float content = 0;
            for (Module m : mods) content += rowsHeight(m);
            int avail = maxH - HEAD_H;
            int bodyH = (int) Math.min(content + PAD * 2, avail);
            float maxScroll = Math.max(0, content + PAD * 2 - avail);
            float sc = Math.max(0, Math.min(maxScroll, scroll.getOrDefault(cat, 0f)));
            scroll.put(cat, sc);

            MenuStyle.rrect(g, x, top, COL_W, HEAD_H + bodyH, 4, MenuStyle.alpha(MenuStyle.PANEL, a));
            MenuStyle.rrect(g, x, top, COL_W, HEAD_H, 4, MenuStyle.alpha(MenuStyle.HEADER, a));
            MenuStyle.gradientH(g, x + 6, top + HEAD_H - 1, COL_W - 12, 1,
                    MenuStyle.alpha(MenuStyle.ACCENT, a), MenuStyle.alpha(MenuStyle.ACCENT2, a));
            g.drawText(tr, cat.label, x + (COL_W - tr.getWidth(cat.label)) / 2, top + 6,
                    MenuStyle.alpha(MenuStyle.TEXT, a), false);

            int clipTop = top + HEAD_H, clipBot = top + HEAD_H + bodyH;
            g.enableScissor(x, clipTop, x + COL_W, clipBot);
            float y = clipTop + PAD - sc;
            for (Module m : mods) y = drawModule(g, tr, m, x, y, clipTop, clipBot, mx, my, dt, a);
            g.disableScissor();
        }

        int sw = 110, sx = (width - sw) / 2, sy = height - 30;
        MenuStyle.rrect(g, sx, sy, sw, 16, 4, MenuStyle.alpha(MenuStyle.HEADER, a));
        String shown = query.isEmpty() ? "Search" : query + ((System.currentTimeMillis() / 500 % 2 == 0) ? "_" : "");
        g.drawText(tr, shown, sx + (sw - tr.getWidth(shown)) / 2, sy + 4,
                MenuStyle.alpha(query.isEmpty() ? MenuStyle.MUTED : MenuStyle.TEXT, a), false);
        String hint = "ЛКМ - вкл/выкл   ПКМ - настройки   СКМ - бинд";
        g.drawText(tr, hint, (width - tr.getWidth(hint)) / 2, sy + 20, MenuStyle.alpha(MenuStyle.MUTED, a * 0.8f), false);
    }

    private float drawModule(DrawContext g, TextRenderer tr, Module m, int x, float y,
                             int clipTop, int clipBot, int mx, int my, float dt, float a) {
        boolean on = m.isEnabled();
        boolean open = expanded.contains(m);
        int rx = x + PAD, rw = COL_W - PAD * 2, ry = Math.round(y);

        float o = MenuStyle.approach(openAnim.getOrDefault(m, 0f), open ? 1f : 0f, 16f, dt);
        if (Math.abs(o - (open ? 1f : 0f)) < 0.002f) o = open ? 1f : 0f;
        openAnim.put(m, o);

        if (ry + ROW_H > clipTop && ry < clipBot) {
            boolean over = mx >= rx && mx < rx + rw && my >= Math.max(ry, clipTop) && my < Math.min(ry + ROW_H, clipBot);
            float hv = MenuStyle.approach(hover.getOrDefault(m, 0f), over ? 1f : 0f, 18f, dt);
            hover.put(m, hv);
            if (on) MenuStyle.rrect(g, rx, ry, rw, ROW_H, 3, MenuStyle.alpha(MenuStyle.ROW_ON, a));
            if (hv > 0.01f) MenuStyle.rrect(g, rx, ry, rw, ROW_H, 3, MenuStyle.alpha(MenuStyle.ROW_HOVER, a * hv));
            int color = on ? MenuStyle.TEXT : MenuStyle.lerp(MenuStyle.MUTED, MenuStyle.TEXT, hv);
            g.drawText(tr, m.name, rx + 4, ry + 4, MenuStyle.alpha(color, a), false);
            String mark = null;
            if (binding == m) mark = "[...]";
            else if (m.key >= 0) mark = "[" + keyName(m.key) + "]";
            else if (!m.settings.isEmpty()) mark = open ? "-" : "...";
            if (mark != null) {
                g.drawText(tr, mark, rx + rw - tr.getWidth(mark) - 4, ry + 4,
                        MenuStyle.alpha(binding == m ? MenuStyle.ACCENT : MenuStyle.MUTED, a), false);
            }
            if (on) g.fill(rx, ry + 3, rx + 1, ry + ROW_H - 3, MenuStyle.alpha(MenuStyle.ACCENT, a));
            hits.add(new Hit(rx, Math.max(ry, clipTop), rw, Math.min(ry + ROW_H, clipBot) - Math.max(ry, clipTop), m, null));
        }
        y += ROW_H;

        float shownH = settingsHeight(m) * o;
        if (shownH > 0.5f) {
            int sTop = Math.max(Math.round(y), clipTop), sBot = Math.min(Math.round(y + shownH), clipBot);
            if (sBot > sTop) {
                g.enableScissor(rx, sTop, rx + rw, sBot);
                float sy = y;
                for (Setting<?> s : m.settings) {
                    if (!s.isVisible()) continue;
                    int h = (s instanceof NumberSetting) ? SLIDER_H : SET_H;
                    int iy = Math.round(sy);
                    drawSetting(g, tr, s, rx + 3, iy, rw - 6, a * o);
                    if (o > 0.9f && iy + h > sTop && iy < sBot) hits.add(new Hit(rx + 3, iy, rw - 6, h, m, s));
                    sy += h;
                }
                g.disableScissor();
            }
        }
        return y + shownH;
    }

    private void drawSetting(DrawContext g, TextRenderer tr, Setting<?> s, int x, int y, int w, float a) {
        int muted = MenuStyle.alpha(MenuStyle.MUTED, a), text = MenuStyle.alpha(MenuStyle.TEXT, a);
        if (s instanceof BoolSetting b) {
            boolean v = b.get();
            g.drawText(tr, s.name, x + 2, y + 3, v ? text : muted, false);
            int px = x + w - 16;
            MenuStyle.rrect(g, px, y + 3, 14, 8, 3, MenuStyle.alpha(v ? MenuStyle.ACCENT : MenuStyle.TRACK, a));
            g.fill(px + (v ? 8 : 2), y + 5, px + (v ? 12 : 6), y + 9, text);
        } else if (s instanceof ModeSetting ms) {
            g.drawText(tr, s.name, x + 2, y + 3, muted, false);
            String v = ms.get();
            g.drawText(tr, v, x + w - tr.getWidth(v) - 2, y + 3, MenuStyle.alpha(MenuStyle.ACCENT, a), false);
        } else if (s instanceof NumberSetting ns) {
            g.drawText(tr, s.name, x + 2, y + 2, muted, false);
            String v = ns.display();
            g.drawText(tr, v, x + w - tr.getWidth(v) - 2, y + 2, text, false);
            int bx = x + 2, bw = w - 4, by = y + 13;
            double t = (ns.get() - ns.min) / Math.max(1e-9, ns.max - ns.min);
            MenuStyle.rrect(g, bx, by, bw, 3, 1, MenuStyle.alpha(MenuStyle.TRACK, a));
            int fw = (int) (bw * Math.max(0, Math.min(1, t)));
            if (fw > 0) MenuStyle.gradientH(g, bx, by, fw, 3, MenuStyle.alpha(MenuStyle.ACCENT, a), MenuStyle.alpha(MenuStyle.ACCENT2, a));
        }
    }

    private List<Module> filtered(Category cat) {
        List<Module> all = ModuleManager.byCategory(cat);
        if (query.isEmpty()) return all;
        List<Module> out = new ArrayList<>();
        String q = query.toLowerCase(Locale.ROOT);
        for (Module m : all) if (m.name.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        return out;
    }

    private float settingsHeight(Module m) {
        float h = 0;
        for (Setting<?> s : m.settings) if (s.isVisible()) h += (s instanceof NumberSetting) ? SLIDER_H : SET_H;
        return h;
    }

    private float rowsHeight(Module m) {
        return ROW_H + settingsHeight(m) * openAnim.getOrDefault(m, 0f);
    }

    private void applySlider(NumberSetting ns, Hit h, int mx) {
        double t = Math.max(0, Math.min(1, (mx - (h.x() + 2)) / (double) (h.w() - 4)));
        double v = ns.min + t * (ns.max - ns.min);
        if (ns.step > 0) v = ns.min + Math.round((v - ns.min) / ns.step) * ns.step;
        ns.set(Math.max(ns.min, Math.min(ns.max, v)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.in(mx, my)) continue;
            if (h.setting() == null) {
                if (button == 0) h.module().toggle();
                else if (button == 2) binding = (binding == h.module()) ? null : h.module();
                else if (button == 1 && !h.module().settings.isEmpty() && !expanded.remove(h.module())) expanded.add(h.module());
            } else if (h.setting() instanceof BoolSetting b && button == 0) {
                b.toggle();
            } else if (h.setting() instanceof ModeSetting ms) {
                ms.cycle(button == 1 ? -1 : 1);
            } else if (h.setting() instanceof NumberSetting ns && button == 0) {
                dragging = h;
                applySlider(ns, h, (int) mx);
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        Category[] cats = Category.values();
        int totalW = cats.length * COL_W + (cats.length - 1) * GAP;
        int idx = (int) Math.floor((mx - (width - totalW) / 2.0) / (COL_W + GAP));
        if (idx >= 0 && idx < cats.length) {
            Category c = cats[idx];
            scroll.put(c, scroll.getOrDefault(c, 0f) - (float) vAmount * 18f);
            return true;
        }
        return super.mouseScrolled(mx, my, hAmount, vAmount);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr >= 32 && query.length() < 20) { query += chr; return true; }
        return super.charTyped(chr, modifiers);
    }

    private String keyName(int key) {
        return keyNames.computeIfAbsent(key, k -> {
            String t = InputUtil.fromKeyCode(k, 0).getTranslationKey();
            int i = t.lastIndexOf('.');
            String n = (i >= 0 ? t.substring(i + 1) : t).toUpperCase(Locale.ROOT);
            return n.length() > 5 ? n.substring(0, 5) : n;
        });
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (binding != null) {
            // Esc / Backspace / Delete - снять бинд
            binding.key = (key == 256 || key == 259 || key == 261) ? -1 : key;
            binding = null;
            return true;
        }
        if (key == 259 && !query.isEmpty()) { query = query.substring(0, query.length() - 1); return true; } // Backspace
        return super.keyPressed(key, scan, mods);
    }
}
