package app.ghostclient.mod.gui;

import app.ghostclient.mod.FriendsClient;
import app.ghostclient.mod.GhostConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Friends HUD Overlay - Shows online friends and recent messages in-game
 * Displays in the top-right corner with smooth animations
 */
public class FriendsHudOverlay {

    private static final int BG = 0xD0000000;
    private static final int CARD_BG = 0xE0141620;
    private static final int ONLINE = 0xFF5AE07B;
    private static final int TEXT_MAIN = 0xFFF5F6FA;
    private static final int TEXT_DIM = 0xFF9BA3B8;
    private static final int ACCENT = 0xFF6B8AFF;

    private static final List<ChatNotification> notifications = new ArrayList<>();
    private static long lastChatCheck = 0;

    private static class ChatNotification {
        final UUID from;
        final String username;
        final String message;
        final long timestamp;
        boolean dismissed;

        ChatNotification(UUID from, String username, String message) {
            this.from = from;
            this.username = username;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
            this.dismissed = false;
        }

        float getAlpha() {
            long age = System.currentTimeMillis() - timestamp;
            if (dismissed) return 0;
            if (age < 200) return age / 200f; // Fade in
            if (age > 4800) return Math.max(0, (5000 - age) / 200f); // Fade out
            return 1f;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 5000 || dismissed;
        }
    }

    public static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.debugEnabled) return; // Don't show on F3 screen

        GhostConfig cfg = GhostConfig.get();

        // Check for new messages
        checkNewMessages();

        // Render friend list HUD
        if (cfg.showFriendsOnHud) {
            renderFriendsHud(ctx, mc);
        }

        // Render chat notifications
        if (cfg.chatPopupEnabled) {
            renderChatNotifications(ctx, mc);
        }

        // Clean up expired notifications
        notifications.removeIf(ChatNotification::isExpired);
    }

    private static void renderFriendsHud(DrawContext ctx, MinecraftClient mc) {
        List<FriendsClient.Friend> onlineFriends = FriendsClient.friends().stream()
            .filter(FriendsClient.Friend::online)
            .limit(5)
            .toList();

        if (onlineFriends.isEmpty()) return;

        int width = mc.getWindow().getScaledWidth();
        int hudX = width - 150;
        int hudY = 10;
        int hudW = 140;
        int hudH = 12 + onlineFriends.size() * 20 + 4;

        // Background
        RoundedGui.rect(ctx, hudX, hudY, hudW, hudH, 6, CARD_BG);

        // Title
        ctx.drawText(mc.textRenderer, Text.literal("Online Friends").formatted(Formatting.BOLD),
            hudX + 6, hudY + 4, TEXT_MAIN, true);

        // Friends list
        int y = hudY + 16;
        for (FriendsClient.Friend friend : onlineFriends) {
            // Online indicator
            ctx.fill(hudX + 8, y + 4, hudX + 12, y + 8, ONLINE);

            // Username
            String name = friend.username();
            if (name.length() > 16) name = name.substring(0, 14) + "..";
            ctx.drawText(mc.textRenderer, name, hudX + 16, y + 2, TEXT_DIM, true);

            y += 20;
        }
    }

    private static void renderChatNotifications(DrawContext ctx, MinecraftClient mc) {
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        int notifX = width - 280;
        int notifY = height - 80;

        int index = 0;
        for (int i = notifications.size() - 1; i >= 0 && index < 3; i--) {
            ChatNotification notif = notifications.get(i);
            float alpha = notif.getAlpha();
            if (alpha <= 0) continue;

            int y = notifY - (index * 70);
            renderChatNotification(ctx, mc, notifX, y, notif, alpha);
            index++;
        }
    }

    private static void renderChatNotification(DrawContext ctx, MinecraftClient mc, int x, int y, ChatNotification notif, float alpha) {
        int notifW = 270;
        int notifH = 60;

        // Background with alpha
        int bgAlpha = (int)(alpha * 224);
        int bgColor = (bgAlpha << 24) | 0x1A1D28;
        RoundedGui.rect(ctx, x, y, notifW, notifH, 8, bgColor);

        // Accent bar on left
        int accentAlpha = (int)(alpha * 255);
        int accentColor = (accentAlpha << 24) | (ACCENT & 0x00FFFFFF);
        RoundedGui.rect(ctx, x, y, 4, notifH, 8, accentColor);

        // Avatar placeholder (colored circle)
        int avatarX = x + 12;
        int avatarY = y + 12;
        int avatarSize = 36;
        int avatarColor = (accentAlpha << 24) | 0x3D5099;
        RoundedGui.rect(ctx, avatarX, avatarY, avatarSize, avatarSize, avatarSize / 2, avatarColor);

        // First letter of username
        String initial = notif.username.substring(0, 1).toUpperCase();
        int textAlpha = (int)(alpha * 255);
        int textColor = (textAlpha << 24) | 0xFFFFFF;
        ctx.drawText(mc.textRenderer, initial, avatarX + 13, avatarY + 14, textColor, true);

        // Username
        int contentX = x + 56;
        ctx.drawText(mc.textRenderer, Text.literal(notif.username).formatted(Formatting.BOLD),
            contentX, y + 8, (textAlpha << 24) | (TEXT_MAIN & 0x00FFFFFF), true);

        // Message preview
        String preview = notif.message;
        if (preview.length() > 35) preview = preview.substring(0, 33) + "...";
        ctx.drawText(mc.textRenderer, preview, contentX, y + 22, (textAlpha << 24) | (TEXT_DIM & 0x00FFFFFF), true);

        // Click to open hint
        ctx.drawText(mc.textRenderer, "Click to reply", contentX, y + 38,
            ((int)(alpha * 128) << 24) | (TEXT_DIM & 0x00FFFFFF), true);
    }

    private static void checkNewMessages() {
        long now = System.currentTimeMillis();
        if (now - lastChatCheck < 1000) return; // Check every second
        lastChatCheck = now;

        // Check for new messages from all friends
        for (FriendsClient.Friend friend : FriendsClient.friends()) {
            List<FriendsClient.ChatMessage> messages = FriendsClient.chatWith(friend.uuid());
            if (messages.isEmpty()) continue;

            // Get the most recent message from them
            FriendsClient.ChatMessage latest = null;
            for (int i = messages.size() - 1; i >= 0; i--) {
                FriendsClient.ChatMessage msg = messages.get(i);
                if (!msg.mine()) {
                    latest = msg;
                    break;
                }
            }

            if (latest != null) {
                // Check if we already have a notification for this message
                boolean alreadyNotified = notifications.stream()
                    .anyMatch(n -> n.from.equals(latest.from()) && n.message.equals(latest.message()));

                if (!alreadyNotified && System.currentTimeMillis() - latest.ts() < 5000) {
                    notifications.add(new ChatNotification(friend.uuid(), friend.username(), latest.message()));
                }
            }
        }
    }

    public static void onChatNotificationClicked(int mouseX, int mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        int notifX = width - 280;
        int notifY = height - 80;

        int index = 0;
        for (int i = notifications.size() - 1; i >= 0 && index < 3; i--) {
            ChatNotification notif = notifications.get(i);
            if (notif.getAlpha() <= 0) continue;

            int y = notifY - (index * 70);
            if (mouseX >= notifX && mouseX <= notifX + 270 && mouseY >= y && mouseY <= y + 60) {
                // Open chat with this friend
                mc.setScreen(new GhostMenuScreenEnhanced());
                notif.dismissed = true;
                return;
            }
            index++;
        }
    }
}
