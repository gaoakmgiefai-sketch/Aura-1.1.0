package com.example.aura.gui;

import com.example.aura.AuraMod;
import com.example.aura.config.ConfigManager;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Меню в стиле «окно»: категории слева, модули в центре, настройки справа. */
public class ClickGui extends Screen {
    private static final int SIDE_W = 92;
    private static final int LIST_W = 150;
    private static final int HEADER_H = 28;

    private interface Handler {
        void click(int button);
    }

    private record Hit(int x, int y, int w, int h, int clipTop, int clipBottom, Handler handler) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h && my >= clipTop && my < clipBottom;
        }
    }

    private final List<Hit> hits = new ArrayList<>();
    private final float[] categoryAnim = new float[Category.values().length];

    private Category category = Category.COMBAT;
    private Module selected;
    private boolean listeningBind;
    private NumberSetting dragging;
    private int dragX, dragW;
    private double lastMouseX;
    private float scroll, targetScroll, maxScroll;
    private float open;
    private long lastNanos = System.nanoTime();

    public ClickGui() {
        super(Text.literal("Aura"));
        selectFirst();
    }

    private void selectFirst() {
        List<Module> list = ModuleManager.byCategory(category);
        selected = list.isEmpty() ? null : list.get(0);
        scroll = targetScroll = 0;
        listeningBind = false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        ConfigManager.save();
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        open = Theme.approach(open, 1f, dt, 14f);
        lastMouseX = mx;
        hits.clear();

        TextRenderer tr = this.textRenderer;
        ctx.fill(0, 0, width, height, ((int) (150 * open)) << 24);

        int ww = Math.min(540, width - 16);
        int wh = Math.min(330, height - 16);
        int x0 = (width - ww) / 2;
        int y0 = (height - wh) / 2 + (int) ((1f - open) * 14f);

        Theme.rrect(ctx, x0 - 1, y0 - 1, ww + 2, wh + 2, Theme.OUTLINE);
        Theme.rrect(ctx, x0, y0, ww, wh, Theme.BG);

        // --- шапка
        ctx.drawText(tr, Text.literal("AURA").formatted(Formatting.BOLD), x0 + 12, y0 + 10, Theme.ACCENT, true);
        ctx.drawText(tr, "v" + AuraMod.VERSION + "  ·  1.21.8", x0 + 12 + tr.getWidth("AURA") + 8, y0 + 10, Theme.MUTED, false);
        Theme.gradientH(ctx, x0 + 1, y0 + HEADER_H - 2, ww - 2, 2, Theme.ACCENT, Theme.ACCENT2);

        drawCategories(ctx, tr, mx, my, x0, y0, dt);
        drawModules(ctx, tr, mx, my, x0, y0, dt);
        drawSettings(ctx, tr, mx, my, x0, y0, ww, wh, dt);

        // разделители
        ctx.fill(x0 + SIDE_W, y0 + HEADER_H + 8, x0 + SIDE_W + 1, y0 + wh - 8, Theme.OUTLINE);
        ctx.fill(x0 + SIDE_W + LIST_W + 6, y0 + HEADER_H + 8, x0 + SIDE_W + LIST_W + 7, y0 + wh - 8, Theme.OUTLINE);

        ctx.drawText(tr, "LMB toggle  ·  RMB select", x0 + 10, y0 + wh - 14, Theme.MUTED, false);
    }

    private void drawCategories(DrawContext ctx, TextRenderer tr, int mx, int my, int x0, int y0, float dt) {
        int sx = x0 + 8;
        int sw = SIDE_W - 16;
        int cy = y0 + HEADER_H + 10;
        for (Category c : Category.values()) {
            boolean sel = c == category;
            boolean hov = inside(mx, my, sx, cy, sw, 24);
            float target = sel ? 1f : hov ? 0.5f : 0f;
            categoryAnim[c.ordinal()] = Theme.approach(categoryAnim[c.ordinal()], target, dt, 16f);
            float a = categoryAnim[c.ordinal()];

            Theme.rrect(ctx, sx, cy, sw, 24, Theme.lerp(Theme.BG, Theme.PANEL_SEL, a));
            if (sel) Theme.rrect(ctx, sx, cy + 5, 3, 14, Theme.ACCENT);
            ctx.drawText(tr, c.label, sx + 11, cy + 8, Theme.lerp(Theme.MUTED, Theme.TEXT, a), false);

            hits.add(new Hit(sx, cy, sw, 24, 0, height, b -> {
                category = c;
                selectFirst();
            }));
            cy += 28;
        }
    }

    private void drawModules(DrawContext ctx, TextRenderer tr, int mx, int my, int x0, int y0, float dt) {
        int lx = x0 + SIDE_W + 6;
        int rowW = LIST_W - 4;
        int ly = y0 + HEADER_H + 10;
        for (Module m : ModuleManager.byCategory(category)) {
            boolean sel = m == selected;
            boolean hov = inside(mx, my, lx, ly, rowW, 26);
            m.anim = Theme.approach(m.anim, m.isEnabled() ? 1f : 0f, dt, 16f);

            Theme.rrect(ctx, lx, ly, rowW, 26, sel ? Theme.PANEL_SEL : hov ? Theme.PANEL_HOV : Theme.PANEL);
            ctx.drawText(tr, m.name, lx + 10, ly + 9, Theme.lerp(Theme.MUTED, Theme.TEXT, m.anim), false);
            Theme.toggle(ctx, lx + rowW - 32, ly + 8, m.anim);

            hits.add(new Hit(lx, ly, rowW, 26, 0, height, b -> {
                if (selected != m) {
                    selected = m;
                    scroll = targetScroll = 0;
                    listeningBind = false;
                }
                if (b == 0) m.toggle();
            }));
            ly += 30;
        }
    }

    private void drawSettings(DrawContext ctx, TextRenderer tr, int mx, int my, int x0, int y0, int ww, int wh, float dt) {
        if (selected == null) return;
        int px = x0 + SIDE_W + LIST_W + 14;
        int pw = x0 + ww - 10 - px;
        int py = y0 + HEADER_H + 10;
        int ph = wh - HEADER_H - 30;
        if (pw < 40 || ph < 20) return;

        scroll = Theme.approach(scroll, targetScroll, dt, 20f);
        ctx.enableScissor(px - 2, py, px + pw + 2, py + ph);

        int top = py - Math.round(scroll);
        int y = top;

        ctx.drawText(tr, Text.literal(selected.name).formatted(Formatting.BOLD), px, y, Theme.TEXT, true);
        y += 12;
        ctx.drawText(tr, tr.trimToWidth(selected.description, pw), px, y, Theme.MUTED, false);
        y += 16;

        // --- бинд
        final Module bindModule = selected;
        String bindText = listeningBind ? "press a key..." : keyName(bindModule.key);
        ctx.drawText(tr, "Keybind", px, y + 6, Theme.MUTED, false);
        int bw = tr.getWidth(bindText) + 12;
        Theme.rrect(ctx, px + pw - bw, y + 1, bw, 16, listeningBind ? Theme.ACCENT : Theme.PANEL_SEL);
        ctx.drawText(tr, bindText, px + pw - bw + 6, y + 5, Theme.TEXT, false);
        hits.add(new Hit(px, y, pw, 18, py, py + ph, b -> listeningBind = !listeningBind));
        y += 24;

        for (Setting<?> s : selected.settings) {
            if (!s.isVisible()) continue;
            if (s instanceof BoolSetting bs) y = drawBool(ctx, tr, bs, px, y, pw, py, ph, dt);
            else if (s instanceof NumberSetting ns) y = drawNumber(ctx, tr, ns, px, y, pw, py, ph);
            else if (s instanceof ModeSetting ms) y = drawMode(ctx, tr, ms, px, y, pw, py, ph);
        }

        maxScroll = Math.max(0, (y - top) - ph + 4);
        targetScroll = MathHelper.clamp(targetScroll, 0, maxScroll);
        ctx.disableScissor();

        // индикатор прокрутки
        if (maxScroll > 0) {
            float ratio = ph / (float) (ph + maxScroll);
            int barH = Math.max(14, (int) (ph * ratio));
            int barY = py + (int) ((ph - barH) * (scroll / maxScroll));
            ctx.fill(px + pw + 3, barY, px + pw + 5, barY + barH, Theme.TRACK);
        }
    }

    private int drawBool(DrawContext ctx, TextRenderer tr, BoolSetting s, int px, int y, int pw, int py, int ph, float dt) {
        s.anim = Theme.approach(s.anim, s.get() ? 1f : 0f, dt, 16f);
        ctx.drawText(tr, s.name, px, y + 6, Theme.lerp(Theme.MUTED, Theme.TEXT, s.anim), false);
        Theme.toggle(ctx, px + pw - 22, y + 5, s.anim);
        hits.add(new Hit(px, y, pw, 20, py, py + ph, b -> s.toggle()));
        return y + 22;
    }

    private int drawNumber(DrawContext ctx, TextRenderer tr, NumberSetting s, int px, int y, int pw, int py, int ph) {
        ctx.drawText(tr, s.name, px, y + 2, Theme.TEXT, false);
        String v = s.display();
        ctx.drawText(tr, v, px + pw - tr.getWidth(v), y + 2, Theme.MUTED, false);

        int ty = y + 16;
        float ratio = (float) ((s.get() - s.min) / (s.max - s.min));
        Theme.rrect(ctx, px, ty, pw, 6, Theme.TRACK);
        int fillW = Math.max(6, Math.round(pw * ratio));
        Theme.rrect(ctx, px, ty, fillW, 6, Theme.lerp(Theme.ACCENT, Theme.ACCENT2, ratio));
        Theme.rrect(ctx, px + Math.round((pw - 8) * ratio), ty - 2, 8, 10, 0xFFFFFFFF);

        hits.add(new Hit(px, y + 12, pw, 16, py, py + ph, b -> {
            if (b != 0) return;
            dragging = s;
            dragX = px;
            dragW = pw;
            updateSlider(lastMouseX);
        }));
        return y + 32;
    }

    private int drawMode(DrawContext ctx, TextRenderer tr, ModeSetting s, int px, int y, int pw, int py, int ph) {
        ctx.drawText(tr, s.name, px, y + 6, Theme.TEXT, false);
        String v = "<  " + s.get() + "  >";
        int w = tr.getWidth(v) + 10;
        Theme.rrect(ctx, px + pw - w, y + 1, w, 16, Theme.PANEL_SEL);
        ctx.drawText(tr, v, px + pw - w + 5, y + 5, Theme.ACCENT2, false);
        hits.add(new Hit(px, y, pw, 20, py, py + ph, b -> s.cycle(b == 1 ? -1 : 1)));
        return y + 22;
    }

    // ------------------------------------------------------------------ input

    private void updateSlider(double mouseX) {
        if (dragging == null || dragW <= 0) return;
        double r = MathHelper.clamp((mouseX - dragX) / dragW, 0.0, 1.0);
        dragging.set(dragging.min + r * (dragging.max - dragging.min));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseX = mouseX;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.contains(mouseX, mouseY)) {
                h.handler().click(button);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null) {
            updateSlider(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        targetScroll = MathHelper.clamp(targetScroll - (float) verticalAmount * 18f, 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningBind && selected != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                selected.key = GLFW.GLFW_KEY_UNKNOWN;
            } else {
                selected.key = keyCode;
            }
            listeningBind = false;
            return true;
        }
        if (AuraMod.menuKey != null && AuraMod.menuKey.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ------------------------------------------------------------------ helpers

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String keyName(int key) {
        if (key < 0) return "None";
        try {
            return InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
        } catch (Exception e) {
            return "Key " + key;
        }
    }
}
