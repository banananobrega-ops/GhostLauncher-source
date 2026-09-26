package app.ghostclient.mod.gui;

import app.ghostclient.mod.FriendsClient;
import app.ghostclient.mod.GhostConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Enhanced Ghost Client Menu - OneConfig style with better customization
 * Features:
 * - Beautiful sidebar navigation
 * - Friends list with online status
 * - Real-time chat
 * - Mod toggles with visual feedback
 * - Cape preview
 */
public class GhostMenuScreenEnhanced extends Screen {

    private enum Tab { MODS, FRIENDS, CAPES, SETTINGS }

    // Enhanced color palette
    private static final int BG_DIM = 0xB0000000;
    private static final int PANEL_BG = 0xFF0F1117;
    private static final int SIDEBAR_BG = 0xFF07080C;
    private static final int CARD_BG = 0xFF1A1D28;
    private static final int CARD_BG_HOVER = 0xFF232836;
    private static final int ACCENT = 0xFF6B8AFF;
    private static final int ACCENT_HOVER = 0xFF8BA3FF;
    private static final int ACCENT_DIM = 0xFF3D5099;
    private static final int OFF_BAR = 0xFF2A2E3B;
    private static final int TEXT_MAIN = 0xFFF5F6FA;
    private static final int TEXT_DIM = 0xFF9BA3B8;
    private static final int TEXT_DARKER = 0xFF6B7280;
    private static final int ONLINE = 0xFF5AE07B;
    private static final int OFFLINE = 0xFF5B6172;
    private static final int BUBBLE_MINE = 0xFF4866DB;
    private static final int BUBBLE_THEIRS = 0xFF272C3D;
    private static final int SUCCESS = 0xFF4CAF50;
    private static final int WARNING = 0xFFFF9800;
    private static final int ERROR = 0xFFF44336;

    private static Tab currentTab = Tab.MODS;
    private static UUID selectedFriend = null;
    private static int scrollOffset = 0;

    private int panelX, panelY, panelW, panelH;
    private int sidebarW = 180;
    private int contentX, contentY, contentW, contentH;

    private TextFieldWidget addFriendField;
    private TextFieldWidget chatField;
    private ButtonWidget addFriendBtn;
    private ButtonWidget sendChatBtn;

    private final List<ClickArea> clickAreas = new ArrayList<>();
    private record ClickArea(int x, int y, int w, int h, Runnable action) {
        boolean hit(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    public GhostMenuScreenEnhanced() {
        super(Text.literal("Ghost Client"));
    }

    @Override
    protected void init() {
        panelW = Math.min(740, this.width - 40);
        panelH = Math.min(480, this.height - 40);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        contentX = panelX + sidebarW + 20;
        contentY = panelY + 24;
        contentW = panelX + panelW - contentX - 24;
        contentH = panelY + panelH - contentY - 24;

        // Friend request field
        addFriendField = new TextFieldWidget(this.textRenderer, contentX, contentY, contentW - 100, 24, Text.literal("username"));
        addFriendField.setMaxLength(32);
        addFriendField.setPlaceholder(Text.literal("Enter friend's username...").formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(addFriendField);

        addFriendBtn = ButtonWidget.builder(Text.literal("Add Friend"), b -> {
            String name = addFriendField.getText();
            if (!name.isBlank()) {
                FriendsClient.sendFriendRequest(name);
                addFriendField.setText("");
            }
        }).dimensions(contentX + contentW - 94, contentY, 94, 24).build();
        this.addDrawableChild(addFriendBtn);

        // Chat input field
        int chatBoxY = contentY + contentH - 28;
        int listW = 160;
        chatField = new TextFieldWidget(this.textRenderer, contentX + listW + 16, chatBoxY, contentW - listW - 16 - 70, 24, Text.literal("message"));
        chatField.setMaxLength(500);
        chatField.setPlaceholder(Text.literal("Type a message...").formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(chatField);

        sendChatBtn = ButtonWidget.builder(Text.literal("Send"), b -> sendChat())
                .dimensions(contentX + contentW - 64, chatBoxY, 64, 24).build();
        this.addDrawableChild(sendChatBtn);

        updateWidgetVisibility();
    }

    private void updateWidgetVisibility() {
        boolean onFriends = currentTab == Tab.FRIENDS;
        addFriendField.setVisible(onFriends);
        addFriendField.active = onFriends;
        addFriendBtn.visible = onFriends;
        addFriendBtn.active = onFriends;
        chatField.setVisible(onFriends);
        chatField.active = onFriends;
        sendChatBtn.visible = onFriends;
        sendChatBtn.active = onFriends;
        if (!onFriends) {
            addFriendField.setFocused(false);
            chatField.setFocused(false);
        }
    }

    private void sendChat() {
        if (chatField == null || selectedFriend == null) return;
        String msg = chatField.getText();
        if (msg.isBlank()) return;
        FriendsClient.sendMessage(selectedFriend, msg);
        chatField.setText("");
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        clickAreas.clear();

        // Background overlay
        ctx.fill(0, 0, this.width, this.height, BG_DIM);

        // Main panel with enhanced shadow
        drawShadow(ctx, panelX, panelY, panelW, panelH);
        RoundedGui.rect(ctx, panelX, panelY, panelW, panelH, 12, PANEL_BG);

        // Sidebar
        RoundedGui.rect(ctx, panelX, panelY, sidebarW, panelH, 12, SIDEBAR_BG);
        ctx.fill(panelX + sidebarW - 12, panelY, panelX + sidebarW, panelY + panelH, SIDEBAR_BG);

        renderSidebar(ctx, mouseX, mouseY);

        switch (currentTab) {
            case MODS -> renderModsEnhanced(ctx, mouseX, mouseY);
            case FRIENDS -> renderFriendsEnhanced(ctx, mouseX, mouseY);
            case CAPES -> renderCapes(ctx, mouseX, mouseY);
            case SETTINGS -> renderSettingsEnhanced(ctx, mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawShadow(DrawContext ctx, int x, int y, int w, int h) {
        int shadowSize = 20;
        for (int i = 0; i < shadowSize; i++) {
            int alpha = (shadowSize - i) * 2;
            int color = (alpha << 24);
            ctx.fill(x - i, y - i, x + w + i, y - i + 1, color);
            ctx.fill(x - i, y + h + i - 1, x + w + i, y + h + i, color);
            ctx.fill(x - i, y - i, x - i + 1, y + h + i, color);
            ctx.fill(x + w + i - 1, y - i, x + w + i, y + h + i, color);
        }
    }

    private void renderSidebar(DrawContext ctx, int mouseX, int mouseY) {
        int x = panelX + 20;
        int y = panelY + 24;

        // Ghost Client branding
        ctx.drawText(this.textRenderer, Text.literal("GHOST").formatted(Formatting.BOLD), x, y, TEXT_MAIN, false);
        ctx.drawText(this.textRenderer, Text.literal("CLIENT").formatted(Formatting.BOLD), x, y + 11, ACCENT, false);

        // Version badge
        RoundedGui.rect(ctx, x, y + 26, 48, 14, 7, ACCENT_DIM);
        ctx.drawText(this.textRenderer, "v1.0.0", x + 6, y + 29, TEXT_MAIN, false);

        int ny = y + 52;
        ny = sidebarItem(ctx, mouseX, mouseY, x, ny, "Mods", Tab.MODS, "");
        ny = sidebarItem(ctx, mouseX, mouseY, x, ny, "Friends", Tab.FRIENDS, "");
        ny = sidebarItem(ctx, mouseX, mouseY, x, ny, "Capes", Tab.CAPES, "");
        ny = sidebarItem(ctx, mouseX, mouseY, x, ny, "Settings", Tab.SETTINGS, "");

        // Online friends count
        int onlineCount = (int) FriendsClient.friends().stream().filter(FriendsClient.Friend::online).count();
        if (onlineCount > 0) {
            int badgeY = panelY + panelH - 60;
            RoundedGui.rect(ctx, x, badgeY, sidebarW - 40, 20, 6, 0xFF1A4D2E);
            ctx.drawText(this.textRenderer, onlineCount + " online", x + 8, badgeY + 6, ONLINE, false);
        }

        // Player info footer
        String name = app.ghostclient.mod.GhostPlayers.ownName();
        if (name.isEmpty()) name = "Player";
        int footY = panelY + panelH - 32;
        RoundedGui.rect(ctx, x, footY, sidebarW - 40, 24, 8, 0xFF14161D);
        ctx.drawText(this.textRenderer, Text.literal(name).formatted(Formatting.BOLD), x + 8, footY + 8, TEXT_MAIN, false);
    }

    private int sidebarItem(DrawContext ctx, int mouseX, int mouseY, int x, int y, String label, Tab tab, String icon) {
        int w = sidebarW - 40;
        int h = 28;
        boolean selected = currentTab == tab;
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;

        if (selected) {
            RoundedGui.rect(ctx, x, y, w, h, 8, ACCENT);
            // Glow effect
            RoundedGui.rect(ctx, x - 2, y - 2, w + 4, h + 4, 10, ACCENT_DIM & 0x40FFFFFF);
        } else if (hover) {
            RoundedGui.rect(ctx, x, y, w, h, 8, CARD_BG_HOVER);
        }

        ctx.drawText(this.textRenderer, Text.literal(label), x + 10, y + 10, selected ? 0xFFFFFFFF : TEXT_DIM, false);

        clickAreas.add(new ClickArea(x, y, w, h, () -> {
            if (currentTab != tab) {
                currentTab = tab;
                scrollOffset = 0;
                updateWidgetVisibility();
            }
        }));
        return y + h + 8;
    }

    // ---------------------------------------------------------------- ENHANCED MODS

    private record ModEntry(String name, String desc, String icon, BooleanSupplier get, Consumer<Boolean> set) {}

    private List<ModEntry> modEntries() {
        GhostConfig cfg = GhostConfig.get();
        List<ModEntry> list = new ArrayList<>();
        list.add(new ModEntry("Capes", "Show custom capes in-game", "C", () -> cfg.showCapes, v -> cfg.showCapes = v));
        list.add(new ModEntry("Badges", "Display Ghost icon on players", "B", () -> cfg.showBadges, v -> cfg.showBadges = v));
        list.add(new ModEntry("Friends HUD", "Show online friends overlay", "F", () -> cfg.showFriendsOnHud, v -> cfg.showFriendsOnHud = v));
        list.add(new ModEntry("Chat Popup", "Real-time message notifications", "M", () -> cfg.chatPopupEnabled, v -> cfg.chatPopupEnabled = v));
        return list;
    }

    private void renderModsEnhanced(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, Text.literal("Mods").formatted(Formatting.BOLD), contentX, contentY - 2, TEXT_MAIN, false);
        ctx.drawText(this.textRenderer, "Customize your Ghost Client experience", contentX, contentY + 12, TEXT_DARKER, false);

        int cardW = 160, cardH = 110, gap = 16;
        int cols = Math.max(1, (contentW + gap) / (cardW + gap));
        int startY = contentY + 36;

        List<ModEntry> entries = modEntries();
        for (int i = 0; i < entries.size(); i++) {
            int col = i % cols, row = i / cols;
            int cx = contentX + col * (cardW + gap);
            int cy = startY + row * (cardH + gap);
            ModEntry entry = entries.get(i);
            boolean on = entry.get().getAsBoolean();
            boolean hover = mouseX >= cx && mouseX < cx + cardW && mouseY >= cy && mouseY < cy + cardH;

            int bgColor = hover ? CARD_BG_HOVER : CARD_BG;
            if (on) {
                drawGlow(ctx, cx, cy, cardW, cardH, 8);
            }
            RoundedGui.rect(ctx, cx, cy, cardW, cardH, 10, bgColor);

            // Icon circle
            int iconD = 40;
            int iconX = cx + cardW / 2 - iconD / 2;
            int iconY = cy + 18;
            RoundedGui.rect(ctx, iconX, iconY, iconD, iconD, iconD / 2, on ? ACCENT : 0xFF1C1F28);
            ctx.drawCenteredTextWithShadow(this.textRenderer, entry.icon(), cx + cardW / 2, iconY + 14, on ? 0xFFFFFFFF : TEXT_DARKER);

            // Name
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(entry.name()).formatted(Formatting.BOLD),
                cx + cardW / 2, cy + 66, TEXT_MAIN);

            // Description
            ctx.drawCenteredTextWithShadow(this.textRenderer, entry.desc(), cx + cardW / 2, cy + 78, TEXT_DARKER);

            // Toggle bar
            int barY = cy + cardH - 8;
            RoundedGui.rect(ctx, cx + 6, barY - 18, cardW - 12, 20, 6, on ? ACCENT : OFF_BAR);
            ctx.drawCenteredTextWithShadow(this.textRenderer, on ? "ENABLED" : "DISABLED",
                cx + cardW / 2, barY - 12, 0xFFFFFFFF);

            final int fx = cx, fy = cy, fw = cardW, fh = cardH;
            clickAreas.add(new ClickArea(fx, fy, fw, fh, () -> {
                entry.set().accept(!entry.get().getAsBoolean());
                GhostConfig.get().save();
            }));
        }
    }

    private void drawGlow(DrawContext ctx, int x, int y, int w, int h, int radius) {
        for (int i = 0; i < 4; i++) {
            int alpha = (4 - i) * 10;
            int color = (alpha << 24) | (ACCENT & 0x00FFFFFF);
            RoundedGui.rect(ctx, x - i, y - i, w + i * 2, h + i * 2, radius + i, color);
        }
    }

    // ---------------------------------------------------------------- ENHANCED FRIENDS

    private void renderFriendsEnhanced(DrawContext ctx, int mouseX, int mouseY) {
        int listW = 160;
        int listX = contentX;
        int listY = contentY + 30;
        int listH = contentH - 30 - 32;

        // Friends list container
        RoundedGui.rect(ctx, listX, listY, listW, listH, 10, CARD_BG);

        // Title
        ctx.drawText(this.textRenderer, Text.literal("Friends").formatted(Formatting.BOLD), listX + 8, listY - 20, TEXT_MAIN, false);

        List<FriendsClient.Request> incoming = FriendsClient.incoming();
        List<FriendsClient.Friend> friends = FriendsClient.friends();

        int rowY = listY + 8;

        // Friend requests
        for (FriendsClient.Request req : incoming) {
            if (rowY + 44 > listY + listH) break;

            RoundedGui.rect(ctx, listX + 6, rowY, listW - 12, 40, 8, 0xFF2A2440);
            ctx.drawText(this.textRenderer, trim(req.username(), 13), listX + 10, rowY + 6, TEXT_MAIN, false);
            ctx.drawText(this.textRenderer, Text.literal("Friend Request").formatted(Formatting.ITALIC),
                listX + 10, rowY + 18, TEXT_DARKER, false);

            int ax = listX + listW - 10 - 18;
            final UUID from = req.uuid();

            // Accept button
            RoundedGui.rect(ctx, ax, rowY + 24, 18, 12, 4, SUCCESS);
            ctx.drawCenteredTextWithShadow(this.textRenderer, "✓", ax + 9, rowY + 26, 0xFFFFFFFF);
            clickAreas.add(new ClickArea(ax, rowY + 24, 18, 12, () -> FriendsClient.acceptFriendRequest(from)));

            // Decline button
            RoundedGui.rect(ctx, ax - 22, rowY + 24, 18, 12, 4, ERROR);
            ctx.drawCenteredTextWithShadow(this.textRenderer, "✗", ax - 13, rowY + 26, 0xFFFFFFFF);
            clickAreas.add(new ClickArea(ax - 22, rowY + 24, 18, 12, () -> FriendsClient.declineFriendRequest(from)));

            rowY += 44;
        }

        if (friends.isEmpty() && incoming.isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("No friends yet").formatted(Formatting.ITALIC),
                listX + listW / 2, rowY + 20, TEXT_DARKER);
        }

        // Friends list
        for (FriendsClient.Friend f : friends) {
            if (rowY + 32 > listY + listH) break;

            boolean selected = f.uuid().equals(selectedFriend);
            boolean hover = mouseX >= listX + 6 && mouseX < listX + listW - 6 && mouseY >= rowY && mouseY < rowY + 30;

            if (selected) {
                RoundedGui.rect(ctx, listX + 6, rowY, listW - 12, 30, 8, ACCENT_DIM);
            } else if (hover) {
                RoundedGui.rect(ctx, listX + 6, rowY, listW - 12, 30, 8, CARD_BG_HOVER);
            }

            // Online status indicator
            RoundedGui.rect(ctx, listX + 12, rowY + 11, 8, 8, 4, f.online() ? ONLINE : OFFLINE);

            // Username
            ctx.drawText(this.textRenderer, trim(f.username(), 14), listX + 26, rowY + 10, TEXT_MAIN, false);

            final UUID uuid = f.uuid();
            clickAreas.add(new ClickArea(listX + 6, rowY, listW - 12, 30, () -> selectedFriend = uuid));
            rowY += 32;
        }

        // Chat panel
        int chatX = listX + listW + 16;
        int chatY = listY;
        int chatW = contentX + contentW - chatX;
        int chatH = listH;
        RoundedGui.rect(ctx, chatX, chatY, chatW, chatH, 10, CARD_BG);

        if (selectedFriend == null) {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Select a friend to chat").formatted(Formatting.ITALIC),
                chatX + chatW / 2, chatY + chatH / 2 - 4, TEXT_DARKER);
            return;
        }

        // Chat messages
        List<FriendsClient.ChatMessage> messages = FriendsClient.chatWith(selectedFriend);
        int msgY = chatY + chatH - 12;
        int maxWidth = chatW - 32;

        for (int i = messages.size() - 1; i >= 0 && msgY > chatY + 8; i--) {
            FriendsClient.ChatMessage m = messages.get(i);
            List<String> lines = wrap(m.message(), maxWidth - 20);
            int bubbleH = 8 + lines.size() * 11;
            int bubbleW = Math.min(maxWidth, Math.max(40, lines.stream().mapToInt(this.textRenderer::getWidth).max().orElse(40) + 18));
            int bx = m.mine() ? chatX + chatW - 16 - bubbleW : chatX + 16;
            int by = msgY - bubbleH;

            RoundedGui.rect(ctx, bx, by, bubbleW, bubbleH, 8, m.mine() ? BUBBLE_MINE : BUBBLE_THEIRS);

            int ty = by + 6;
            for (String line : lines) {
                ctx.drawText(this.textRenderer, line, bx + 9, ty, TEXT_MAIN, false);
                ty += 11;
            }
            msgY = by - 6;
        }
    }

    // ---------------------------------------------------------------- CAPES

    private void renderCapes(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, Text.literal("Capes").formatted(Formatting.BOLD), contentX, contentY, TEXT_MAIN, false);
        ctx.drawText(this.textRenderer, "Upload and manage your custom capes", contentX, contentY + 14, TEXT_DARKER, false);

        int cardY = contentY + 40;
        RoundedGui.rect(ctx, contentX, cardY, contentW, 120, 10, CARD_BG);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("Cape Upload").formatted(Formatting.BOLD),
            contentX + contentW / 2, cardY + 20, TEXT_MAIN);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
            "Go to the launcher to upload capes",
            contentX + contentW / 2, cardY + 40, TEXT_DARKER);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
            "Capes will be visible to all Ghost Client users",
            contentX + contentW / 2, cardY + 55, TEXT_DARKER);

        // Cape info
        int infoY = cardY + 85;
        ctx.drawCenteredTextWithShadow(this.textRenderer,
            "Supported formats: PNG • Max size: 64x32 or multiples",
            contentX + contentW / 2, infoY, TEXT_DARKER);
    }

    // ---------------------------------------------------------------- ENHANCED SETTINGS

    private void renderSettingsEnhanced(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, Text.literal("Settings").formatted(Formatting.BOLD), contentX, contentY, TEXT_MAIN, false);

        int y = contentY + 24;

        // Info card
        RoundedGui.rect(ctx, contentX, y, contentW, 80, 10, CARD_BG);
        ctx.drawText(this.textRenderer, Text.literal("Ghost Client v1.0.0").formatted(Formatting.BOLD), contentX + 16, y + 16, ACCENT, false);
        ctx.drawText(this.textRenderer, "Enhanced Minecraft client mod", contentX + 16, y + 32, TEXT_DIM, false);
        ctx.drawText(this.textRenderer, "Press Right Shift to toggle this menu", contentX + 16, y + 46, TEXT_DARKER, false);

        // Network status
        y += 96;
        String error = FriendsClient.lastError();
        if (error != null) {
            RoundedGui.rect(ctx, contentX, y, contentW, 60, 10, 0xFF2A1A1A);
            ctx.drawText(this.textRenderer, Text.literal("Network Error").formatted(Formatting.BOLD), contentX + 16, y + 14, ERROR, false);
            ctx.drawText(this.textRenderer, trim(error, 60).getString(), contentX + 16, y + 32, TEXT_DIM, false);
        } else {
            RoundedGui.rect(ctx, contentX, y, contentW, 60, 10, 0xFF1A2A1A);
            ctx.drawText(this.textRenderer, Text.literal("Connected").formatted(Formatting.BOLD), contentX + 16, y + 14, SUCCESS, false);
            ctx.drawText(this.textRenderer, "All systems operational", contentX + 16, y + 32, TEXT_DIM, false);
        }
    }

    // ---------------------------------------------------------------- INPUT

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (ClickArea area : clickAreas) {
                if (area.hit(mouseX, mouseY)) {
                    area.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private static Text trim(String s, int max) {
        if (s.length() > max) s = s.substring(0, max - 1) + "...";
        return Text.literal(s);
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = cur.isEmpty() ? word : cur + " " + word;
            if (this.textRenderer.getWidth(candidate) > maxWidth && !cur.isEmpty()) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(candidate);
            }
        }
        if (!cur.isEmpty()) lines.add(cur.toString());
        if (lines.isEmpty()) lines.add("");
        return lines;
    }
}
