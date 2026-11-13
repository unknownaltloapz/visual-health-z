package com.visual.client.mixin;

import java.lang.reflect.Field;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class HealthLabelMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void visualShowHealthLabel(T entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LivingEntity living) {
            if (living.isAlive() && !(living instanceof ArmorStand) && squaredDistanceToCamera < 1024.0) {
                cir.setReturnValue(true);
            }
        }
    }
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true, require = 0)
    private void visualAppendHealthToNameTag(T entity, CallbackInfoReturnable<Component> cir) {
        if (entity instanceof LivingEntity living) {
            if (!living.isAlive() || living instanceof ArmorStand) {
                return;
            }
            Component base = cir.getReturnValue();
            if (base == null) {
                base = living.getDisplayName();
            }
            if (base == null) {
                return;
            }
            String plain = base.getString();
            if (plain.contains("❤")) {
                return;
            }
            cir.setReturnValue(visualBuildLabel(living, base));
        }
    }
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void visualAppendHealthToRenderState(T entity, S state, float tickProgress, CallbackInfo ci) {
        if (entity instanceof LivingEntity living) {
            if (!living.isAlive() || living instanceof ArmorStand) {
                return;
            }
            try {
                Field nameField = visualFindField(state.getClass(), Component.class);
                if (nameField == null) {
                    return;
                }
                Component current = (Component) nameField.get(state);
                if (current != null && current.getString().contains("❤")) {
                    return;
                }
                Component base = current;
                if (base == null) {
                    base = living.getDisplayName();
                }
                if (base == null) {
                    return;
                }
                nameField.set(state, visualBuildLabel(living, base));
            } catch (Exception ignored) {
            }
        }
    }
    private static Field visualFindField(Class<?> start, Class<?> type) {
        Class<?> current = start;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType() == type) {
                    field.setAccessible(true);
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }
    private static Component visualBuildLabel(LivingEntity living, Component base) {
        float health = living.getHealth();
        float maxHealth = living.getMaxHealth();
        if (maxHealth <= 0.0F) {
            return base;
        }
        float clamped = Math.max(0.0F, Math.min(health, maxHealth));
        float percent = clamped / maxHealth;
        ChatFormatting color = percent > 0.6F ? ChatFormatting.GREEN : percent > 0.3F ? ChatFormatting.YELLOW : ChatFormatting.RED;
        int hearts = Math.round(clamped / 2.0F);
        int maxHearts = Math.round(maxHealth / 2.0F);
        MutableComponent result = Component.empty().append(base).append(Component.literal(" "));
        MutableComponent healthText = Component.literal("❤ " + visualFormatHealth(clamped) + " / " + visualFormatHealth(maxHealth) + " (" + hearts + "/" + maxHearts + " ♥)").withStyle(color);
        result.append(healthText);
        int filled = Math.round(percent * 10.0F);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        result.append(Component.literal(" "));
        result.append(Component.literal(bar.toString()).withStyle(color));
        float absorption = living.getAbsorptionAmount();
        if (absorption > 0.0F) {
            result.append(Component.literal(" +" + visualFormatHealth(absorption)).withStyle(ChatFormatting.GOLD));
        }
        return result;
    }
    private static String visualFormatHealth(float value) {
        float rounded = Math.round(value * 10.0F) / 10.0F;
        if (rounded == Math.floor(rounded)) {
            return String.valueOf((int) rounded);
        }
        return String.valueOf(rounded);
    }
}