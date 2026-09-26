package app.ghostclient.mod.gui;

import app.ghostclient.mod.FriendsClient;
import app.ghostclient.mod.GhostConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ghost Client Purple Menu - Modern purple/dark theme inspired by advanced UIs
 * Features beautiful purple gradients, smooth animations, and professional design
 */
public class GhostMenuScreenPurple extends Screen {

    private enum Tab { MODS, FRIENDS, CAPES, SETTINGS }

    // Modern Purple Color Palette
    private static final int BG_OVERLAY = 0xC0000000;          // Semi-transparent black overlay
    private static final int PANEL_BG = 0xFF0A0B0F;            // Very dark blue-grey
    private static final int SIDEBAR_BG = 0xFF06070A;          // Almost black sidebar
    private static final int CARD_BG = 0xFF12141B;             // Dark card background
    private static final int CARD_HOVER = 0xFF1A1D28;          // Lighter on hover
    private static final int ACCENT_PRIMARY = 0xFF8B5CF6;      // Beautiful purple
    private static final int ACCENT_HOVER = 0xFFA78BFA;        // Lighter purple hover
    private static final int ACCENT_DIM = 0xFF6D28D9;          // Darker purple
    private static final int ACCENT_GLOW = 0x40A78BFA;         // Glow effect
    private static final int TEXT_PRIMARY = 0xFFF3F4F6;        // Almost white
    private static final int TEXT_SECONDARY = 0xFFD1D5DB;      // Light grey
    private static final int TEXT_TERTIARY = 0xFF9CA3AF;       // Medium grey
    private static final int TEXT_DIM = 0xFF6B7280;            // Dark grey
    private static final int ONLINE = 0xFF10B981;              // Green online
    private static final int OFFLINE = 0xFF6B7280;             // Grey offline
    private static final int DIVIDER = 0xFF1F2937;             // Subtle divider
    private static final int CHAT_MINE = 0xFF7C3AED;           // Purple for my messages
    private static final int CHAT_THEIRS = 0xFF1F2937;         // Dark grey for their messages
    private static final int SUCCESS = 0xFF10B981;
    private static final int WARNING = 0xFFF59E0B;
    private static final int ERROR = 0xFFEF4444;

    private static Tab currentTab = Tab.MODS;
    private static UUID selectedFriend = null;
    private static int scrollOffset = 0;
    private static String searchQuery = "";

    private int panelX, panelY, panelW, panelH;
    private int sidebarW = 200;
    private int contentX, contentY, contentW, contentH;

    private TextFieldWidget addFriendField;
    private TextFieldWidget chatField;
    private TextFieldWidget searchField;
    private ButtonWidget addFriendBtn;
    private ButtonWidget sendChatBtn;

    private final List<ClickArea> clickAreas = new ArrayList<>();
    private int hoverIndex = -1;

    private record ClickArea(int x, int y, int w, int h, Runnable action) {
        boolean hit(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    public GhostMenuScreenPurple() {
        super(Text.literal("Ghost Client"));
    }

    @Override
    protected void init() {
        // Calculate panel dimensions
        panelW = Math.min(900, this.width - 60);
        panelH = Math.min(560, this.height - 60);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        contentX = panelX + sidebarW + 24;
        contentY = panelY + 70;
        contentW = panelX + panelW - contentX - 24;
        contentH = panelY + panelH - contentY - 24;

        clickAreas.clear();

        // Initialize widgets based on current tab
        if (currentTab == Tab.FRIENDS) {
            initFriendsWidgets();
        } else if (currentTab == Tab.MODS) {
            initModsWidgets();
        }
    }

    private void initFriendsWidgets() {
        // Search field
        searchField = new TextFieldWidget(this.textRenderer, contentX, contentY - 36, 200, 28, Text.literal("search"));
        searchField.setMaxLength(32);
        searchField.setPlaceholder(Text.literal("Search friends...").formatted(Formatting.DARK_GRAY));
        searchField.setText(searchQuery);
        searchField.setChangedListener(s -> searchQuery = s);
        this.addDrawableChild(searchField);

        if (selectedFriend == null) {
            // Add friend input
            addFriendField = new TextFieldWidget(this.textRenderer, contentX, contentY + contentH - 32, contentW - 110, 28, Text.literal("username"));
            addFriendField.setMaxLength(32);
            addFriendField.setPlaceholder(Text.literal("Enter friend's username...").formatted(Formatting.DARK_GRAY));
            this.addDrawableChild(addFriendField);

            addFriendBtn = ButtonWidget.builder(Text.literal("Add Friend"), b -> {
                String name = addFriendField.getText();
                if (!name.isBlank()) {
                    FriendsClient.sendFriendRequest(name);
                    addFriendField.setText("");
                }
            }).dimensions(contentX + contentW - 104, contentY + contentH - 32, 104, 28).build();
            this.addDrawableChild(addFriendBtn);
        } else {
            // Chat input
            chatField = new TextFieldWidget(this.textRenderer, contentX, contentY + contentH - 32, contentW - 110, 28, Text.literal("chat"));
            chatField.setMaxLength(256);
            chatField.setPlaceholder(Text.literal("Type a message...").formatted(Formatting.DARK_GRAY));
            this.addDrawableChild(chatField);

            sendChatBtn = ButtonWidget.builder(Text.literal("Send"), b -> {
                String msg = chatField.getText();
                if (!msg.isBlank()) {
                    FriendsClient.sendMessage(selectedFriend, msg);
                    chatField.setText("");
                }
            }).dimensions(contentX + contentW - 104, contentY + contentH - 32, 104, 28).build();
            this.addDrawableChild(sendChatBtn);
        }
    }

    private void initModsWidgets() {
        // Search field for mods
        searchField = new TextFieldWidget(this.textRenderer, contentX, contentY - 36, 200, 28, Text.literal("search"));
        searchField.setMaxLength(32);
        searchField.setPlaceholder(Text.literal("Search mods...").formatted(Formatting.DARK_GRAY));
        searchField.setText(searchQuery);
        searchField.setChangedListener(s -> searchQuery = s);
        this.addDrawableChild(searchField);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Background overlay
        ctx.fill(0, 0, this.width, this.height, BG_OVERLAY);

        // Main panel with subtle glow
        drawGlowRect(ctx, panelX - 2, panelY - 2, panelW + 4, panelH + 4, ACCENT_GLOW);
        drawRoundedRect(ctx, panelX, panelY, panelW, panelH, 12, PANEL_BG);

        // Render sidebar
        renderSidebar(ctx, mouseX, mouseY);

        // Header bar
        renderHeader(ctx, mouseX, mouseY);

        // Content area
        renderContent(ctx, mouseX, mouseY);

        // Render widgets on top
        super.render(ctx, mouseX, mouseY, delta);

        // Bottom status bar
        renderStatusBar(ctx);
    }

    private void renderSidebar(DrawContext ctx, int mouseX, int mouseY) {
        // Sidebar background
        drawRoundedRectPartial(ctx, panelX, panelY, sidebarW, panelH, 12, SIDEBAR_BG, true, false);

        int y = panelY + 20;

        // Ghost Client Logo/Title
        ctx.drawText(this.textRenderer, "GHOST", panelX + 20, y, ACCENT_PRIMARY, true);
        ctx.drawText(this.textRenderer, "CLIENT", panelX + 20, y + 12, TEXT_SECONDARY, true);

        y += 50;

        // Navigation tabs
        Tab[] tabs = Tab.values();
        String[] labels = {"Mods", "Friends", "Capes", "Settings"};
        String[] icons = {"⚙", "👥", "🎨", "⚙"};

        for (int i = 0; i < tabs.length; i++) {
            boolean selected = currentTab == tabs[i];
            boolean hovered = mouseX >= panelX + 12 && mouseX < panelX + sidebarW - 12
                           && mouseY >= y && mouseY < y + 36;

            int tabBg = selected ? CARD_HOVER : (hovered ? CARD_BG : 0);
            int textColor = selected ? TEXT_PRIMARY : TEXT_TERTIARY;
            int accentColor = selected ? ACCENT_PRIMARY : 0;

            if (tabBg != 0) {
                drawRoundedRect(ctx, panelX + 12, y, sidebarW - 24, 36, 8, tabBg);
            }

            // Accent bar on left for selected tab
            if (selected) {
                drawRoundedRect(ctx, panelX + 12, y + 8, 3, 20, 2, ACCENT_PRIMARY);
            }

            // Icon and label
            ctx.drawText(this.textRenderer, icons[i], panelX + 26, y + 12, textColor, false);
            ctx.drawText(this.textRenderer, labels[i], panelX + 46, y + 12, textColor, false);

            final Tab tab = tabs[i];
            clickAreas.add(new ClickArea(panelX + 12, y, sidebarW - 24, 36, () -> switchTab(tab)));

            y += 40;
        }

        // Version info at bottom
        y = panelY + panelH - 30;
        ctx.drawText(this.textRenderer, "v1.0.0", panelX + 20, y, TEXT_DIM, false);
    }

    private void renderHeader(DrawContext ctx, int mouseX, int mouseY) {
        int headerX = panelX + sidebarW;
        int headerY = panelY;
        int headerW = panelW - sidebarW;
        int headerH = 60;

        // Header background
        ctx.fill(headerX, headerY, headerX + headerW, headerY + headerH, CARD_BG);

        // Title
        String title = switch (currentTab) {
            case MODS -> "Mod Manager";
            case FRIENDS -> selectedFriend == null ? "Friends" : getFriendName(selectedFriend);
            case CAPES -> "Cape Customization";
            case SETTINGS -> "Settings";
        };

        ctx.drawText(this.textRenderer, title, headerX + 24, headerY + 22, TEXT_PRIMARY, true);

        // Close button
        int closeX = headerX + headerW - 40;
        int closeY = headerY + 20;
        boolean closeHover = mouseX >= closeX && mouseX < closeX + 24 && mouseY >= closeY && mouseY < closeY + 24;

        drawRoundedRect(ctx, closeX, closeY, 24, 24, 6, closeHover ? ERROR : CARD_HOVER);
        ctx.drawText(this.textRenderer, "×", closeX + 8, closeY + 6, TEXT_PRIMARY, false);
        clickAreas.add(new ClickArea(closeX, closeY, 24, 24, this::close));

        // Back button for friend chat
        if (currentTab == Tab.FRIENDS && selectedFriend != null) {
            int backX = headerX + headerW - 70;
            int backY = headerY + 20;
            boolean backHover = mouseX >= backX && mouseX < backX + 24 && mouseY >= backY && mouseY < backY + 24;

            drawRoundedRect(ctx, backX, backY, 24, 24, 6, backHover ? CARD_HOVER : CARD_BG);
            ctx.drawText(this.textRenderer, "←", backX + 8, backY + 6, TEXT_PRIMARY, false);
            clickAreas.add(new ClickArea(backX, backY, 24, 24, () -> selectedFriend = null));
        }
    }

    private void renderContent(DrawContext ctx, int mouseX, int mouseY) {
        switch (currentTab) {
            case MODS -> renderModsTab(ctx, mouseX, mouseY);
            case FRIENDS -> renderFriendsTab(ctx, mouseX, mouseY);
            case CAPES -> renderCapesTab(ctx, mouseX, mouseY);
            case SETTINGS -> renderSettingsTab(ctx, mouseX, mouseY);
        }
    }

    private void renderModsTab(DrawContext ctx, int mouseX, int mouseY) {
        int y = contentY;
        int cardH = 60;
        int gap = 12;

        // Get all mods
        List<ModItem> mods = getModsList();

        // Filter by search
        String search = searchQuery.toLowerCase();
        if (!search.isEmpty()) {
            mods = mods.stream()
                .filter(m -> m.name.toLowerCase().contains(search))
                .toList();
        }

        for (int i = 0; i < mods.size() && y + cardH <= contentY + contentH; i++) {
            ModItem mod = mods.get(i);
            boolean hover = mouseX >= contentX && mouseX < contentX + contentW
                         && mouseY >= y && mouseY < y + cardH;

            // Card background
            drawRoundedRect(ctx, contentX, y, contentW, cardH, 8, hover ? CARD_HOVER : CARD_BG);

            // Mod icon/indicator
            int iconSize = 40;
            drawRoundedRect(ctx, contentX + 12, y + 10, iconSize, iconSize, 6, ACCENT_DIM);
            ctx.drawCenteredTextWithShadow(this.textRenderer, mod.icon,
                contentX + 12 + iconSize / 2, y + 10 + iconSize / 2 - 4, TEXT_PRIMARY);

            // Mod name and description
            ctx.drawText(this.textRenderer, mod.name, contentX + 64, y + 14, TEXT_PRIMARY, true);
            ctx.drawText(this.textRenderer, mod.description, contentX + 64, y + 30, TEXT_TERTIARY, false);

            // Toggle switch
            int switchX = contentX + contentW - 60;
            int switchY = y + 20;
            renderToggleSwitch(ctx, switchX, switchY, mod.enabled, hover);

            final int index = i;
            clickAreas.add(new ClickArea(switchX, switchY, 44, 24, () -> toggleMod(index)));

            y += cardH + gap;
        }

        if (mods.isEmpty()) {
            String msg = search.isEmpty() ? "No mods available" : "No mods found";
            ctx.drawCenteredTextWithShadow(this.textRenderer, msg,
                contentX + contentW / 2, contentY + contentH / 2, TEXT_DIM);
        }
    }

    private void renderFriendsTab(DrawContext ctx, int mouseX, int mouseY) {
        if (selectedFriend == null) {
            renderFriendsList(ctx, mouseX, mouseY);
        } else {
            renderFriendChat(ctx, mouseX, mouseY);
        }
    }

    private void renderFriendsList(DrawContext ctx, int mouseX, int mouseY) {
        int y = contentY;
        int cardH = 56;
        int gap = 8;

        List<FriendsClient.Friend> friends = FriendsClient.getFriends();

        // Filter by search
        String search = searchQuery.toLowerCase();
        if (!search.isEmpty()) {
            friends = friends.stream()
                .filter(f -> f.name().toLowerCase().contains(search))
                .toList();
        }

        // Sort: online first
        friends = friends.stream()
            .sorted((a, b) -> Boolean.compare(isOnline(b), isOnline(a)))
            .toList();

        for (FriendsClient.Friend friend : friends) {
            if (y + cardH > contentY + contentH - 40) break;

            boolean online = isOnline(friend);
            boolean hover = mouseX >= contentX && mouseX < contentX + contentW
                         && mouseY >= y && mouseY < y + cardH;

            // Card
            drawRoundedRect(ctx, contentX, y, contentW, cardH, 8, hover ? CARD_HOVER : CARD_BG);

            // Status indicator
            int statusSize = 12;
            drawCircle(ctx, contentX + 18, y + 28, statusSize / 2, online ? ONLINE : OFFLINE);

            // Friend name
            ctx.drawText(this.textRenderer, friend.name(), contentX + 36, y + 16, TEXT_PRIMARY, true);

            // Status text
            String status = online ? "Online" : "Offline";
            ctx.drawText(this.textRenderer, status, contentX + 36, y + 32, online ? ONLINE : TEXT_DIM, false);

            // Message button
            if (online) {
                int btnX = contentX + contentW - 80;
                int btnY = y + 16;
                boolean btnHover = mouseX >= btnX && mouseX < btnX + 70 && mouseY >= btnY && mouseY < btnY + 24;

                drawRoundedRect(ctx, btnX, btnY, 70, 24, 6, btnHover ? ACCENT_HOVER : ACCENT_PRIMARY);
                ctx.drawCenteredTextWithShadow(this.textRenderer, "Message",
                    btnX + 35, btnY + 8, TEXT_PRIMARY);

                final UUID friendId = friend.id();
                clickAreas.add(new ClickArea(btnX, btnY, 70, 24, () -> selectedFriend = friendId));
            }

            y += cardH + gap;
        }

        if (friends.isEmpty()) {
            String msg = search.isEmpty() ? "No friends yet" : "No friends found";
            ctx.drawCenteredTextWithShadow(this.textRenderer, msg,
                contentX + contentW / 2, contentY + contentH / 2 - 20, TEXT_DIM);
        }
    }

    private void renderFriendChat(DrawContext ctx, int mouseX, int mouseY) {
        List<FriendsClient.ChatMessage> messages = FriendsClient.getMessages(selectedFriend);

        int y = contentY + contentH - 70;
        int maxY = contentY;

        // Render messages from bottom to top
        for (int i = messages.size() - 1; i >= 0 && y > maxY; i--) {
            FriendsClient.ChatMessage msg = messages.get(i);
            boolean isMine = msg.fromMe();

            List<String> wrapped = wrapText(msg.text(), contentW - 100);
            int msgH = wrapped.size() * 12 + 16;

            y -= msgH + 8;
            if (y < maxY) break;

            int msgX = isMine ? contentX + 60 : contentX + 10;
            int msgW = contentW - 70;

            // Message bubble
            drawRoundedRect(ctx, msgX, y, msgW, msgH, 8, isMine ? CHAT_MINE : CHAT_THEIRS);

            // Message text
            int textY = y + 8;
            for (String line : wrapped) {
                ctx.drawText(this.textRenderer, line, msgX + 8, textY, TEXT_PRIMARY, false);
                textY += 12;
            }
        }

        if (messages.isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, "No messages yet. Say hi!",
                contentX + contentW / 2, contentY + contentH / 2, TEXT_DIM);
        }
    }

    private void renderCapesTab(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Cape customization coming soon!",
            contentX + contentW / 2, contentY + contentH / 2, TEXT_DIM);
    }

    private void renderSettingsTab(DrawContext ctx, int mouseX, int mouseY) {
        int y = contentY;
        int gap = 16;

        // Settings list
        List<Setting> settings = getSettings();

        for (Setting setting : settings) {
            if (y + 40 > contentY + contentH) break;

            // Setting card
            drawRoundedRect(ctx, contentX, y, contentW, 40, 8, CARD_BG);

            // Setting name
            ctx.drawText(this.textRenderer, setting.name, contentX + 16, y + 14, TEXT_PRIMARY, false);

            // Toggle or button
            if (setting.type == SettingType.TOGGLE) {
                int switchX = contentX + contentW - 60;
                int switchY = y + 8;
                renderToggleSwitch(ctx, switchX, switchY, setting.value, false);

                clickAreas.add(new ClickArea(switchX, switchY, 44, 24, setting.action));
            }

            y += 40 + gap;
        }
    }

    private void renderToggleSwitch(DrawContext ctx, int x, int y, boolean enabled, boolean hover) {
        int w = 44;
        int h = 24;

        // Background
        int bg = enabled ? ACCENT_PRIMARY : CARD_HOVER;
        if (hover && !enabled) bg = DIVIDER;
        drawRoundedRect(ctx, x, y, w, h, 12, bg);

        // Knob
        int knobX = enabled ? x + w - 20 : x + 4;
        int knobY = y + 4;
        drawCircle(ctx, knobX + 8, knobY + 8, 8, TEXT_PRIMARY);
    }

    private void renderStatusBar(DrawContext ctx) {
        int barY = panelY + panelH - 28;
        int barH = 28;

        ctx.fill(panelX + sidebarW, barY, panelX + panelW, barY + barH, SIDEBAR_BG);

        // Online status
        String status = FriendsClient.isAuthenticated() ? "Connected" : "Disconnected";
        int statusColor = FriendsClient.isAuthenticated() ? ONLINE : OFFLINE;

        drawCircle(ctx, panelX + sidebarW + 20, barY + 14, 4, statusColor);
        ctx.drawText(this.textRenderer, status, panelX + sidebarW + 32, barY + 10, TEXT_TERTIARY, false);

        // Friend count
        if (FriendsClient.isAuthenticated()) {
            int onlineCount = (int) FriendsClient.getFriends().stream().filter(this::isOnline).count();
            String friendText = onlineCount + " online";
            ctx.drawText(this.textRenderer, friendText, panelX + sidebarW + 130, barY + 10, TEXT_TERTIARY, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (ClickArea area : clickAreas) {
                if (area.hit(mouseX, mouseY)) {
                    area.action.run();
                    init();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = Math.max(0, scrollOffset - (int) (verticalAmount * 20));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            this.close();
            return true;
        }
        if (chatField != null && chatField.isFocused() && keyCode == 257) { // ENTER
            sendChatBtn.onPress();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // Helper methods

    private void switchTab(Tab tab) {
        currentTab = tab;
        searchQuery = "";
        if (tab != Tab.FRIENDS) {
            selectedFriend = null;
        }
    }

    private void toggleMod(int index) {
        List<ModItem> mods = getModsList();
        if (index >= 0 && index < mods.size()) {
            ModItem mod = mods.get(index);
            mod.enabled = !mod.enabled;
            // Apply toggle logic here
        }
    }

    private List<ModItem> getModsList() {
        List<ModItem> mods = new ArrayList<>();
        mods.add(new ModItem("Ghost Badge", "Show Ghost icon next to players", "👻", GhostConfig.showBadge, () -> GhostConfig.showBadge = !GhostConfig.showBadge));
        mods.add(new ModItem("Cape Renderer", "Display custom capes", "🎨", GhostConfig.showCapes, () -> GhostConfig.showCapes = !GhostConfig.showCapes));
        mods.add(new ModItem("Friends HUD", "Show online friends overlay", "👥", GhostConfig.showFriendsHud, () -> GhostConfig.showFriendsHud = !GhostConfig.showFriendsHud));
        mods.add(new ModItem("Chat Notifications", "Get chat notifications", "💬", GhostConfig.showChatNotifications, () -> GhostConfig.showChatNotifications = !GhostConfig.showChatNotifications));
        mods.add(new ModItem("Tab Badge", "Show badge in tab list", "📋", GhostConfig.showTabBadge, () -> GhostConfig.showTabBadge = !GhostConfig.showTabBadge));
        return mods;
    }

    private List<Setting> getSettings() {
        List<Setting> settings = new ArrayList<>();
        settings.add(new Setting("Auto-connect Friends", SettingType.TOGGLE, true, () -> {}));
        settings.add(new Setting("Badge Color: Purple", SettingType.TOGGLE, true, () -> {}));
        settings.add(new Setting("Notification Sounds", SettingType.TOGGLE, false, () -> {}));
        settings.add(new Setting("Show Typing Indicator", SettingType.TOGGLE, true, () -> {}));
        return settings;
    }

    private boolean isOnline(FriendsClient.Friend friend) {
        String status = FriendsClient.getPresence(friend.id());
        return status != null && (status.equals("online") || status.equals("in_game"));
    }

    private String getFriendName(UUID friendId) {
        return FriendsClient.getFriends().stream()
            .filter(f -> f.id().equals(friendId))
            .map(FriendsClient.Friend::name)
            .findFirst()
            .orElse("Friend");
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (this.textRenderer.getWidth(test) <= maxWidth) {
                if (!line.isEmpty()) line.append(" ");
                line.append(word);
            } else {
                if (!line.isEmpty()) lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    // Drawing utilities

    private void drawRoundedRect(DrawContext ctx, int x, int y, int w, int h, int radius, int color) {
        // Simplified rounded rectangle
        ctx.fill(x + radius, y, x + w - radius, y + h, color);
        ctx.fill(x, y + radius, x + radius, y + h - radius, color);
        ctx.fill(x + w - radius, y + radius, x + w, y + h - radius, color);
        // Corners would need circle drawing
        ctx.fill(x, y, x + radius, y + radius, color);
        ctx.fill(x + w - radius, y, x + w, y + radius, color);
        ctx.fill(x, y + h - radius, x + radius, y + h, color);
        ctx.fill(x + w - radius, y + h - radius, x + w, y + h, color);
    }

    private void drawRoundedRectPartial(DrawContext ctx, int x, int y, int w, int h, int radius, int color, boolean leftRounded, boolean rightRounded) {
        if (leftRounded && rightRounded) {
            drawRoundedRect(ctx, x, y, w, h, radius, color);
        } else {
            ctx.fill(x, y, x + w, y + h, color);
        }
    }

    private void drawGlowRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
    }

    private void drawCircle(DrawContext ctx, int cx, int cy, int radius, int color) {
        ctx.fill(cx - radius, cy - radius, cx + radius, cy + radius, color);
    }

    // Data classes

    private static class ModItem {
        String name;
        String description;
        String icon;
        boolean enabled;
        Runnable toggle;

        ModItem(String name, String description, String icon, boolean enabled, Runnable toggle) {
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.enabled = enabled;
            this.toggle = toggle;
        }
    }

    private enum SettingType { TOGGLE, BUTTON }

    private record Setting(String name, SettingType type, boolean value, Runnable action) {}
}
