package com.visual.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;

public class HitTargetHud {
    private static LivingEntity target;
    private static long lastHitTime;
    private static final long SHOW_MS = 2000L;
    public static void onHit(LivingEntity entity) {
        if (entity == null || entity instanceof ArmorStand) {
            return;
        }
        target = entity;
        lastHitTime = System.currentTimeMillis();
    }
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        if (target == null) {
            return;
        }
        Minecraft game = Minecraft.getInstance();
        if (game.player == null || game.level == null) {
            return;
        }
        if (!target.isAlive() || target.isRemoved()) {
            target = null;
            return;
        }
        if (System.currentTimeMillis() - lastHitTime > SHOW_MS) {
            target = null;
            return;
        }
        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        if (maxHealth <= 0.0F) {
            return;
        }
        float clamped = Math.max(0.0F, Math.min(health, maxHealth));
        float percent = clamped / maxHealth;
        int screenWidth = game.getWindow().getGuiScaledWidth();
        int screenHeight = game.getWindow().getGuiScaledHeight();
        int hotbarLeft = (screenWidth - 182) / 2;
        int slot5Center = hotbarLeft + 4 * 20 + 10;
        int centerX = slot5Center;
        int baseY = screenHeight - 64;
        Component name = target.getDisplayName();
        int nameWidth = game.font.width(name);
        int nameX = centerX - nameWidth / 2;
        graphics.text(game.font, name, nameX, baseY - 12, 0xFFFFFFFF, true);
        String healthText = "❤ " + visualFormatHealth(clamped) + " / " + visualFormatHealth(maxHealth);
        ChatFormatting healthColor = percent > 0.6F ? ChatFormatting.GREEN : percent > 0.3F ? ChatFormatting.YELLOW : ChatFormatting.RED;
        MutableComponent healthComponent = Component.literal(healthText).withStyle(healthColor);
        float absorption = target.getAbsorptionAmount();
        if (absorption > 0.0F) {
            healthComponent.append(Component.literal(" +" + visualFormatHealth(absorption)).withStyle(ChatFormatting.GOLD));
        }
        int healthWidth = game.font.width(healthComponent);
        int healthX = centerX - healthWidth / 2;
        graphics.text(game.font, healthComponent, healthX, baseY, 0xFFFFFFFF, true);
    }
    private static String visualFormatHealth(float value) {
        float rounded = Math.round(value * 10.0F) / 10.0F;
        if (rounded == Math.floor(rounded)) {
            return String.valueOf((int) rounded);
        }
        return String.valueOf(rounded);
    }
}