package com.example.aura.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

/** Плавное наведение: экспоненциальное сглаживание + ограничение угловой скорости за тик. */
public final class SmoothRotation {
    private SmoothRotation() {}

    /**
     * @param smoothing 0..0.98 (больше = плавнее/медленнее)
     * @param maxStep   максимум градусов за тик
     */
    public static void rotate(ClientPlayerEntity p, float targetYaw, float targetPitch, float smoothing, float maxStep) {
        float k = 1f - MathHelper.clamp(smoothing, 0f, 0.98f);
        float dy = MathHelper.wrapDegrees(targetYaw - p.getYaw());
        float dp = targetPitch - p.getPitch();
        float sy = MathHelper.clamp(dy * k, -maxStep, maxStep);
        float sp = MathHelper.clamp(dp * k, -maxStep, maxStep);
        p.setYaw(p.getYaw() + sy);
        p.setPitch(MathHelper.clamp(p.getPitch() + sp, -90f, 90f));
    }
}
