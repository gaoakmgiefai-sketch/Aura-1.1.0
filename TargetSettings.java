package com.example.aura.util;

import com.example.aura.modules.AntiBot;
import com.example.aura.module.Module;
import com.example.aura.setting.BoolSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class TargetSettings {
    public final BoolSetting players = new BoolSetting("Players", true);
    public final BoolSetting hostiles = new BoolSetting("Hostiles", true);
    public final BoolSetting passive = new BoolSetting("Passive", false);
    public final BoolSetting invisible = new BoolSetting("Invisible", false);
    public final BoolSetting ignoreTeam = new BoolSetting("Ignore teammates", true);

    public void addTo(Module module) {
        module.add(players);
        module.add(hostiles);
        module.add(passive);
        module.add(invisible);
        module.add(ignoreTeam);
    }

    public boolean matches(Entity e, ClientPlayerEntity self) {
        if (!(e instanceof LivingEntity living) || e == self || !living.isAlive()) return false;
        if (e instanceof ArmorStandEntity) return false;
        if (e instanceof PlayerEntity p) {
            if (p.isSpectator() || AntiBot.isBot(p)) return false;
        }
        if (e.isInvisible() && !invisible.get()) return false;
        if (ignoreTeam.get() && self != null && self.isTeammate(e)) return false;
        if (e instanceof PlayerEntity) return players.get();
        if (e instanceof Monster) return hostiles.get();
        if (e instanceof PassiveEntity) return passive.get();
        return false;
    }
}
