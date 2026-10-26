package com.example.aura.modules;

import com.example.aura.module.Category;
import com.example.aura.module.Module;
import com.example.aura.module.ModuleManager;
import com.example.aura.setting.BoolSetting;
import com.example.aura.setting.ModeSetting;
import com.example.aura.setting.NumberSetting;
import com.example.aura.util.CombatUtil;
import com.example.aura.util.RotationUtil;
import com.example.aura.util.TargetSettings;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KillAura с двумя режимами:
 *  - Legit: плавный доворот с рандомом и GCD-фиксом, удар только если прицел реально на хитбоксе,
 *           рандомный кулдаун/задержка, шанс промаха, пауза при еде.
 *  - HvH:   мгновенная (silent) ротация пакетом, большой радиус, переключение целей, минимальные задержки.
 */
public class KillAura extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", 0, "Legit", "HvH"));
    private final ModeSetting sort = add(new ModeSetting("Sort", 0, "Distance", "Health", "FOV"));

    // --- Legit ---
    private final NumberSetting legitRange = add(new NumberSetting("Legit range", 3.0, 2.0, 4.0, 0.05), this::legit);
    private final NumberSetting fov = add(new NumberSetting("FOV", 140, 30, 360, 5), this::legit);
    private final NumberSetting aimSpeed = add(new NumberSetting("Aim speed", 55, 10, 180, 1), this::legit);
    private final NumberSetting aimJitter = add(new NumberSetting("Aim jitter", 0.6, 0, 3, 0.1), this::legit);
    private final BoolSetting mustLook = add(new BoolSetting("Must look at target", true), this::legit);
    private final NumberSetting missChance = add(new NumberSetting("Miss chance %", 2, 0, 20, 1), this::legit);
    private final NumberSetting clickJitter = add(new NumberSetting("Click jitter (ticks)", 1, 0, 3, 1), this::legit);

    // --- HvH ---
    private final NumberSetting hvhRange = add(new NumberSetting("HvH range", 4.2, 3.0, 6.0, 0.1), () -> !legit());
    private final BoolSetting silent = add(new BoolSetting("Silent rotations", true), () -> !legit());
    private final ModeSetting targeting = add(new ModeSetting("Targeting", 0, "Single", "Switch"), () -> !legit());
    private final NumberSetting hvhDelay = add(new NumberSetting("Attack delay (ticks)", 0, 0, 10, 1), () -> !legit());
    private final NumberSetting hvhCooldown = add(new NumberSetting("Min cooldown", 0.9, 0.0, 1.0, 0.05), () -> !legit());

    // --- Общее / обходы ---
    private final BoolSetting wallCheck = add(new BoolSetting("Wall check", true));
    private final BoolSetting gcdFix = add(new BoolSetting("GCD fix", true));
    private final BoolSetting pauseUse = add(new BoolSetting("Pause while eating", true));
    private final TargetSettings targets = new TargetSettings();

    private LivingEntity target;
    private int hitDelay;
    private float nextCooldown = 0.95f;
    private int switchIndex;
    private float wantYaw, wantPitch;
    private Vec3d aimOffset = Vec3d.ZERO;
    private int offsetTicks;
    private LivingEntity offsetOwner;

    public KillAura() {
        super("KillAura", "Auto attack: Legit or HvH", Category.COMBAT);
        targets.addTo(this);
        key = org.lwjgl.glfw.GLFW.GLFW_KEY_R;
    }

    private boolean legit() {
        return mode.is("Legit");
    }

    @Override
    public String getSuffix() {
        return mode.get();
    }

    @Override
    public void onDisable() {
        target = null;
        offsetOwner = null;
    }

    private boolean canRun(ClientPlayerEntity p) {
        return mc.currentScreen == null
                && mc.interactionManager != null
                && mc.getNetworkHandler() != null
                && p.isAlive()
                && !p.isSpectator()
                && !(pauseUse.get() && p.isUsingItem());
    }

    // ---------------------------------------------------------------- tick: ротация

    @Override
    public void onPreTick() {
        target = null;
        ClientPlayerEntity p = mc.player;
        if (p == null || !canRun(p)) return;
        if (hitDelay > 0) hitDelay--;

        List<LivingEntity> list = collect(p);
        if (list.isEmpty()) return;

        LivingEntity t;
        if (!legit() && targeting.is("Switch")) t = list.get(Math.floorMod(switchIndex, list.size()));
        else t = list.get(0);
        target = t;

        Vec3d aim = aimPoint(p, t);
        float[] rot = RotationUtil.toTarget(p.getEyePos(), aim);
        wantYaw = rot[0];
        wantPitch = rot[1];

        if (legit()) {
            smoothRotate(p);
        } else if (!silent.get()) {
            float dy = MathHelper.wrapDegrees(wantYaw - p.getYaw());
            float dp = wantPitch - p.getPitch();
            if (gcdFix.get()) {
                dy = RotationUtil.gcdFix(dy);
                dp = RotationUtil.gcdFix(dp);
            }
            p.setYaw(p.getYaw() + dy);
            p.setPitch(MathHelper.clamp(p.getPitch() + dp, -90f, 90f));
        }
    }

    private void smoothRotate(ClientPlayerEntity p) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        float dy = MathHelper.wrapDegrees(wantYaw - p.getYaw());
        float dp = wantPitch - p.getPitch();
        float total = (float) Math.hypot(dy, dp);
        if (total > 0.01f) {
            float speed = aimSpeed.floatValue() * (0.7f + rnd.nextFloat() * 0.3f);
            float k = Math.min(1f, speed / total);
            dy *= k;
            dp *= k;
        }
        float jitter = aimJitter.floatValue();
        if (jitter > 0) {
            dy += (rnd.nextFloat() - 0.5f) * jitter;
            dp += (rnd.nextFloat() - 0.5f) * jitter * 0.6f;
        }
        if (gcdFix.get()) {
            dy = RotationUtil.gcdFix(dy);
            dp = RotationUtil.gcdFix(dp);
        }
        p.setYaw(p.getYaw() + dy);
        p.setPitch(MathHelper.clamp(p.getPitch() + dp, -90f, 90f));
    }

    /** Точка внутри хитбокса. В Legit-режиме смещается случайно и обновляется раз в несколько тиков. */
    private Vec3d aimPoint(ClientPlayerEntity p, LivingEntity t) {
        Box b = t.getBoundingBox();
        double h = b.maxY - b.minY;
        double halfW = (b.maxX - b.minX) * 0.5;
        Vec3d eye = p.getEyePos();

        if (legit()) {
            if (offsetOwner != t || --offsetTicks <= 0) {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                aimOffset = new Vec3d(
                        (rnd.nextDouble() - 0.5) * halfW * 0.7,
                        (rnd.nextDouble() - 0.5) * h * 0.2,
                        (rnd.nextDouble() - 0.5) * halfW * 0.7);
                offsetTicks = 6 + rnd.nextInt(10);
                offsetOwner = t;
            }
        } else {
            aimOffset = Vec3d.ZERO;
        }

        double y = MathHelper.clamp(eye.y, b.minY + h * 0.2, b.maxY - h * 0.1);
        Vec3d c = b.getCenter();
        return new Vec3d(c.x + aimOffset.x, y + aimOffset.y, c.z + aimOffset.z);
    }

    // ---------------------------------------------------------------- tick: атака

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        LivingEntity t = target;
        if (p == null || t == null || !canRun(p) || !t.isAlive()) return;
        if (hitDelay > 0) return;

        boolean legit = legit();
        double range = legit ? legitRange.get() : hvhRange.get();
        if (RotationUtil.distance(p.getEyePos(), t) > range) return;

        float needCooldown = legit ? nextCooldown : hvhCooldown.floatValue();
        if (p.getAttackCooldownProgress(0.5f) < needCooldown) return;
        if (legit && mustLook.get() && !RotationUtil.isLookingAt(p, t, range)) return;

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // Намеренный «промах» — просто взмах рукой.
        if (legit && rnd.nextInt(100) < missChance.intValue()) {
            p.swingHand(Hand.MAIN_HAND);
            rollDelay(rnd);
            return;
        }

        Criticals crit = ModuleManager.get(Criticals.class);
        if (!crit.allowAttack(t)) return;

        if (!legit && silent.get()) {
            // Silent: сервер видит нужный угол только на момент удара, потом возвращаем реальный.
            sendLook(p, wantYaw, wantPitch);
            CombatUtil.attack(mc, t);
            sendLook(p, p.getYaw(), p.getPitch());
        } else {
            CombatUtil.attack(mc, t);
        }

        if (!legit) switchIndex++;
        rollDelay(rnd);
    }

    private void rollDelay(ThreadLocalRandom rnd) {
        if (legit()) {
            int j = clickJitter.intValue();
            hitDelay = j > 0 ? rnd.nextInt(j + 1) : 0;
            nextCooldown = 0.92f + rnd.nextFloat() * 0.08f;
        } else {
            hitDelay = hvhDelay.intValue();
        }
    }

    private void sendLook(ClientPlayerEntity p, float yaw, float pitch) {
        mc.getNetworkHandler().sendPacket(
                new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, p.isOnGround(), p.horizontalCollision));
    }

    // ---------------------------------------------------------------- выбор целей

    private List<LivingEntity> collect(ClientPlayerEntity p) {
        boolean legit = legit();
        // В Legit-режиме начинаем доворачиваться чуть раньше, чем цель входит в радиус удара.
        double acquire = (legit ? legitRange.get() + 0.6 : hvhRange.get());
        Vec3d eye = p.getEyePos();
        List<LivingEntity> out = new ArrayList<>();

        for (Entity e : mc.world.getEntities()) {
            if (!targets.matches(e, p)) continue;
            LivingEntity le = (LivingEntity) e;
            if (RotationUtil.distance(eye, le) > acquire) continue;
            if (legit && RotationUtil.yawDifference(p, le) > fov.get() / 2.0) continue;
            if (wallCheck.get() && !p.canSee(le)) continue;
            out.add(le);
        }

        Comparator<LivingEntity> cmp = switch (sort.get()) {
            case "Health" -> Comparator.comparingDouble(LivingEntity::getHealth);
            case "FOV" -> Comparator.comparingDouble(le -> RotationUtil.yawDifference(p, le));
            default -> Comparator.comparingDouble(le -> RotationUtil.distance(eye, le));
        };
        out.sort(cmp);
        return out;
    }
}
