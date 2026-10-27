package com.example.aura.modules;

import com.example.aura.gui.MenuStyle;
import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.setting.BoolSetting;
import com.example.aura.setting.NumberSetting;
import com.example.aura.util.TargetSettings;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.util.Locale;

/** Подсветка цели под прицелом + плавная панель Target HUD (имя, HP, дистанция). */
public class TargetESP extends Module {
    private final TargetSettings targets = new TargetSettings();
    private final NumberSetting range = add(new NumberSetting("Range", 12, 3, 64, 1));
    private final BoolSetting glow = add(new BoolSetting("Glow", true));
    private final BoolSetting hud = add(new BoolSetting("Target HUD", true));

    private LivingEntity target, glowed;
    private String targetName = "";
    private long seenMs;
    private float shownHp, alpha;
    private long lastNs = System.nanoTime();

    public TargetESP() {
        super("Target ESP", "Highlights and shows info about your target", Category.VISUAL);
        targets.addTo(this);
        HudRenderCallback.EVENT.register(this::renderHud);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        Entity looked = mc.targetedEntity;
        if (looked instanceof LivingEntity le && targets.matches(le, p) && le.distanceTo(p) <= range.floatValue()) {
            if (le != target) { target = le; targetName = le.getName().getString(); shownHp = le.getHealth(); }
            seenMs = System.currentTimeMillis();
        } else if (target != null && (!target.isAlive() || target.distanceTo(p) > range.floatValue()
                || System.currentTimeMillis() - seenMs > 1500)) {
            target = null;
        }

        LivingEntity want = glow.get() ? target : null;
        if (glowed != want) {
            if (glowed != null) glowed.setGlowing(false);
            if (want != null && !want.isGlowing()) { want.setGlowing(true); glowed = want; } else glowed = null;
        }
    }

    @Override
    public void onDisable() {
        if (glowed != null) glowed.setGlowing(false);
        glowed = null;
        target = null;
    }

    private void renderHud(DrawContext g, RenderTickCounter tc) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        if (!isEnabled() || !hud.get() || mc.player == null || mc.options.hudHidden) return;

        boolean show = target != null;
        alpha = MenuStyle.approach(alpha, show ? 1f : 0f, 14f, dt);
        if (alpha < 0.02f || target == null) return;

        float max = Math.max(1f, target.getMaxHealth());
        shownHp = MenuStyle.approach(shownHp, Math.min(target.getHealth(), max), 10f, dt);

        int x = mc.getWindow().getScaledWidth() / 2 + 28;
        int y = mc.getWindow().getScaledHeight() / 2 + 10;
        int w = 112, h = 36;
        MenuStyle.rrect(g, x, y, w, h, 4, MenuStyle.alpha(MenuStyle.PANEL, alpha));
        g.drawText(mc.textRenderer, targetName, x + 6, y + 5, MenuStyle.alpha(MenuStyle.TEXT, alpha), false);
        String dist = String.format(Locale.ROOT, "%.1fm", mc.player.distanceTo(target));
        g.drawText(mc.textRenderer, dist, x + w - mc.textRenderer.getWidth(dist) - 6, y + 5,
                MenuStyle.alpha(MenuStyle.MUTED, alpha), false);

        int bx = x + 6, by = y + 19, bw = w - 12;
        MenuStyle.rrect(g, bx, by, bw, 5, 2, MenuStyle.alpha(MenuStyle.TRACK, alpha));
        int fw = Math.round(bw * Math.max(0f, Math.min(1f, shownHp / max)));
        if (fw > 0) MenuStyle.gradientH(g, bx, by, fw, 5, MenuStyle.alpha(MenuStyle.ACCENT, alpha), MenuStyle.alpha(MenuStyle.ACCENT2, alpha));
        String hp = String.format(Locale.ROOT, "%.1f / %.0f", target.getHealth(), max);
        g.drawText(mc.textRenderer, hp, x + 6, y + 26, MenuStyle.alpha(MenuStyle.MUTED, alpha), false);
    }
}
