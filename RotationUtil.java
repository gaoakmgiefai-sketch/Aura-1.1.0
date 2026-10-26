package com.example.aura.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RotationUtil {
    private RotationUtil() {}

    /** @return {yaw, pitch} от точки from к точке to. */
    public static float[] toTarget(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
    }

    public static Vec3d closestPoint(Box box, Vec3d p) {
        return new Vec3d(
                MathHelper.clamp(p.x, box.minX, box.maxX),
                MathHelper.clamp(p.y, box.minY, box.maxY),
                MathHelper.clamp(p.z, box.minZ, box.maxZ));
    }

    /** Расстояние от глаз до ближайшей точки хитбокса (так же считает сервер). */
    public static double distance(Vec3d eye, Entity target) {
        return eye.distanceTo(closestPoint(target.getBoundingBox(), eye));
    }

    /** Модуль разницы yaw между взглядом игрока и направлением на цель. */
    public static float yawDifference(ClientPlayerEntity self, Entity target) {
        Vec3d eye = self.getEyePos();
        Vec3d c = target.getBoundingBox().getCenter();
        float yawTo = toTarget(eye, c)[0];
        return Math.abs(MathHelper.wrapDegrees(yawTo - self.getYaw()));
    }

    /** Попадает ли текущий взгляд игрока в хитбокс цели на дистанции range. */
    public static boolean isLookingAt(ClientPlayerEntity self, Entity target, double range) {
        Vec3d eye = self.getEyePos();
        Vec3d end = eye.add(self.getRotationVec(1.0f).multiply(range));
        Box box = target.getBoundingBox();
        return box.contains(eye) || box.raycast(eye, end).isPresent();
    }

    /**
     * Привязка дельты ротации к сетке мыши (GCD fix). Настоящие повороты мышью
     * всегда кратны этому шагу, «идеальные» углы — нет.
     */
    public static float gcdFix(float delta) {
        double sens = MinecraftClient.getInstance().options.getMouseSensitivity().getValue();
        double f = sens * 0.6 + 0.2;
        double step = f * f * f * 8.0 * 0.15;
        if (step <= 0) return delta;
        return (float) (Math.round(delta / step) * step);
    }
}
