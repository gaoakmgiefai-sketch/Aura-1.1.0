package com.example.aura.modules;

import com.example.aura.AuraMod;
import com.example.aura.gui.Theme;
import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.module.ModuleManager;
import com.example.aura.setting.BoolSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HudModule extends Module {
    private final BoolSetting watermark = add(new BoolSetting("Watermark", true));
    private final BoolSetting moduleList = add(new BoolSetting("Module list", true));

    public HudModule() {
        super("HUD", "Watermark and enabled modules list", Category.VISUAL);
        setEnabledQuiet(true);
    }

    private String label(Module m) {
        String s = m.getSuffix();
        return s == null ? m.name : m.name + " " + s;
    }

    public void render(DrawContext ctx) {
        if (!isEnabled() || mc.options.hudHidden || mc.player == null) return;
        TextRenderer tr = mc.textRenderer;

        if (watermark.get()) {
            String text = "Aura " + AuraMod.VERSION + " | " + mc.getCurrentFps() + " fps";
            int w = tr.getWidth(text);
            Theme.rrect(ctx, 4, 4, w + 10, 15, 0xB0101014);
            ctx.fill(4, 4, 6, 19, Theme.ACCENT);
            ctx.drawText(tr, text, 10, 8, Theme.TEXT, true);
        }

        if (moduleList.get()) {
            List<Module> on = new ArrayList<>();
            for (Module m : ModuleManager.all()) if (m.isEnabled() && m != this) on.add(m);
            on.sort(Comparator.comparingInt((Module m) -> -tr.getWidth(label(m))));

            int sw = ctx.getScaledWindowWidth();
            int y = 4;
            for (int i = 0; i < on.size(); i++) {
                Module m = on.get(i);
                String name = m.name;
                String suffix = m.getSuffix();
                int w = tr.getWidth(label(m));
                int x = sw - w - 7;
                float t = on.size() <= 1 ? 0f : (float) i / (on.size() - 1);
                int color = Theme.lerp(Theme.ACCENT, Theme.ACCENT2, t);

                ctx.fill(x - 3, y - 1, sw, y + 10, 0x90101014);
                ctx.fill(sw - 2, y - 1, sw, y + 10, color);
                ctx.drawText(tr, name, x, y, color, true);
                if (suffix != null) {
                    ctx.drawText(tr, suffix, x + tr.getWidth(name + " "), y, Theme.MUTED, true);
                }
                y += 11;
            }
        }
    }
}
