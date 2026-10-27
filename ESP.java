package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.setting.NumberSetting;
import com.example.aura.util.TargetSettings;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/** ESP через клиентское свечение сущностей: видно сквозь стены, почти без нагрузки на FPS. */
public class ESP extends Module {
    private final TargetSettings targets = new TargetSettings();
    private final NumberSetting range = add(new NumberSetting("Range", 64, 8, 128, 4));
    private final List<Entity> glowing = new ArrayList<>();
    private int tick;

    public ESP() {
        super("ESP", "Highlights entities through walls", Category.VISUAL);
        targets.addTo(this);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;
        if (tick++ % 3 != 0) return; // обновляем раз в 3 тика

        double r2 = range.get() * range.get();
        glowing.removeIf(e -> {
            if (e.isRemoved() || e.squaredDistanceTo(p) > r2 || !targets.matches(e, p)) { e.setGlowing(false); return true; }
            return false;
        });
        for (Entity e : mc.world.getEntities()) {
            if (e.squaredDistanceTo(p) > r2 || !targets.matches(e, p)) continue;
            if (!e.isGlowing()) { e.setGlowing(true); glowing.add(e); }
        }
    }

    @Override
    public void onDisable() {
        for (Entity e : glowing) e.setGlowing(false);
        glowing.clear();
    }
}
