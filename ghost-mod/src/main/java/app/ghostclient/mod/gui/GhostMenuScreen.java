package app.ghostclient.mod.gui;

import app.ghostclient.mod.FriendsClient;
import app.ghostclient.mod.GhostConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Menu do Ghost Client (abre/fecha com Right Shift). Estilo "OneConfig": barra
 * lateral com separadores, painel de conteudo arredondado a direita.
 */
public class GhostMenuScreen extends Screen {

    private enum Tab { MODS, FRIENDS, SETTINGS }

    private static final int BG_DIM = 0xB0000000;
    private static final int PANEL_BG = 0xFF14161F;
    private static final int SIDEBAR_BG = 0xFF0D0E14;
    private static final int CARD_BG = 0xFF1D2130;
    private static final int CARD_BG_HOVER = 0xFF262B3D;
    private static final int ACCENT = 0xFF5B7FFF;
    private static final int ACCENT_DIM = 0xFF33406B;
    private static final int OFF_BAR = 0xFF2A2E3B;
    private static final int TEXT_MAIN = 0xFFF2F3F7;
    private static final int TEXT_DIM = 0xFF8B93A7;
    private static final int ONLINE = 0xFF57D97B;
    private static final int OFFLINE = 0xFF5B6172;
    private static final int BUBBLE_MINE = 0xFF3E56C4;
    private static final int BUBBLE_THEIRS = 0xFF262B3D;

    private static Tab currentTab = Tab.MODS;
    private static UUID selectedFriend = null;

    private int panelX, panelY, panelW, panelH;
    private int sidebarW = 168;
    private int contentX, contentY, contentW, contentH;

    private TextFieldWidget addFriendField;
    private TextFieldWidget chatField;
    private ButtonWidget addFriendBtn;
    private ButtonWidget sendChatBtn;

    private final List<ClickArea> clickAreas = new ArrayList<>();
    private record ClickArea(int x, int y, int w, int h, Runnable action) {
        boolean hit(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    public GhostMenuScreen() {
        super(Text.literal("Ghost Client"));
    }

    @Override
    protected void init() {
        panelW = Math.min(680, this.width - 40);
        panelH = Math.min(420, this.height - 40);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        contentX = panelX + sidebarW + 16;
        contentY = panelY + 20;
        contentW = panelX + panelW - contentX - 20;
        contentH = panelY + panelH - contentY - 20;

        // cria os widgets uma vez; escondemo-los/mostramo-los ao trocar de separador
        // em vez de os remover, para nao depender de Screen#remove(...) que varia de versao.
        addFriendField = new TextFieldWidget(this.textRenderer, contentX, contentY, contentW - 90, 20, Text.literal("username"));
        addFriendField.setMaxLength(32);
        addFriendField.setPlaceholder(Text.literal("nome do teu amigo...").formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(addFriendField);

        addFriendBtn = ButtonWidget.builder(Text.literal("Adicionar"), b -> {
            String name = addFriendField.getText();
            if (!name.isBlank()) {
                FriendsClient.sendFriendRequest(name);
                addFriendField.setText("");
            }
        }).dimensions(contentX + contentW - 84, contentY, 84, 20).build();
        this.addDrawableChild(addFriendBtn);

        int chatBoxY = contentY + contentH - 26;
        int listW = 150;
        chatField = new TextFieldWidget(this.textRenderer, contentX + listW + 12, chatBoxY, contentW - listW - 12 - 60, 20, Text.literal("mensagem"));
        chatField.setMaxLength(500);
        chatField.setPlaceholder(Text.literal("escreve uma mensagem...").formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(chatField);

        sendChatBtn = ButtonWidget.builder(Text.literal("Enviar"), b -> sendChat())
                .dimensions(contentX + contentW - 56, chatBoxY, 56, 20).build();
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
    public boolean keyPressed(KeyInput input) {
        // Enter no campo de chat = enviar
        int keyCode = input.key();
        if (chatField != null && chatField.isFocused() && (keyCode == 257 || keyCode == 335)) {
            sendChat();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        clickAreas.clear();
        ctx.fill(0, 0, this.width, this.height, BG_DIM);

        RoundedGui.rect(ctx, panelX, panelY, panelW, panelH, 10, PANEL_BG);
        RoundedGui.rect(ctx, panelX, panelY, sidebarW, panelH, 10, SIDEBAR_BG);
        // tapa o canto arredondado do lado de dentro da sidebar para ela colar ao painel
        ctx.fill(panelX + sidebarW - 10, panelY, panelX + sidebarW, panelY + panelH, SIDEBAR_BG);

        renderSidebar(ctx, mouseX, mouseY);

        switch (currentTab) {
            case MODS -> renderMods(ctx, mouseX, mouseY);
            case FRIENDS -> renderFriends(ctx, mouseX, mouseY);
            case SETTINGS -> renderSettings(ctx, mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderSidebar(DrawContext ctx, int mouseX, int mouseY) {
        int x = panelX + 16;
        int y = panelY + 18;

        ctx.drawText(this.textRenderer, Text.literal("GHOST").formatted(Formatting.BOLD), x, y, TEXT_MAIN, false);
        ctx.drawText(this.textRenderer, Text.literal("CLIENT").formatted(Formatting.BOLD), x, y + 10, ACCENT, false);

        int ny = y + 34;
        ny = sidebarItem(ctx, mouseX, mouseY, x, ny, "Mods", Tab.MODS);
        ny = sidebarItem(ctx, mouseX, mouseY, x, ny, "Amigos", Tab.FRIENDS);
        ny = sidebarItem(ctx, mouseX, mouseY, x, ny, "Definicoes", Tab.SETTINGS);

        // rodape com o nome do jogador
        String name = app.ghostclient.mod.GhostPlayers.ownName();
        if (name.isEmpty()) name = "?";
        int footY = panelY + panelH - 26;
        ctx.drawText(this.textRenderer, Text.literal(name).formatted(Formatting.GRAY), x, footY, TEXT_DIM, false);
    }

    private int sidebarItem(DrawContext ctx, int mouseX, int mouseY, int x, int y, String label, Tab tab) {
        int w = sidebarW - 32;
        int h = 22;
        boolean selected = currentTab == tab;
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        if (selected) {
            RoundedGui.rect(ctx, x, y, w, h, 6, ACCENT);
        } else if (hover) {
            RoundedGui.rect(ctx, x, y, w, h, 6, CARD_BG_HOVER);
        }
        ctx.drawText(this.textRenderer, Text.literal(label), x + 8, y + 7, selected ? 0xFFFFFFFF : TEXT_DIM, false);
        clickAreas.add(new ClickArea(x, y, w, h, () -> {
            if (currentTab != tab) {
                currentTab = tab;
                updateWidgetVisibility();
            }
        }));
        return y + h + 6;
    }

    // ---------------------------------------------------------------- MODS

    private record ModEntry(String name, String icon, BooleanSupplier get, Consumer<Boolean> set) {}

    private List<ModEntry> modEntries() {
        GhostConfig cfg = GhostConfig.get();
        List<ModEntry> list = new ArrayList<>();
        list.add(new ModEntry("Capas", "C", () -> cfg.showCapes, v -> cfg.showCapes = v));
        list.add(new ModEntry("Badges", "B", () -> cfg.showBadges, v -> cfg.showBadges = v));
        list.add(new ModEntry("Amigos no HUD", "A", () -> cfg.showFriendsOnHud, v -> cfg.showFriendsOnHud = v));
        list.add(new ModEntry("Chat pop-up", "P", () -> cfg.chatPopupEnabled, v -> cfg.chatPopupEnabled = v));
        return list;
    }

    private void renderMods(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, Text.literal("Mods").formatted(Formatting.BOLD), contentX, contentY, TEXT_MAIN, false);

        int cardW = 130, cardH = 92, gap = 12;
        int cols = Math.max(1, (contentW + gap) / (cardW + gap));
        int startY = contentY + 18;

        List<ModEntry> entries = modEntries();
        for (int i = 0; i < entries.size(); i++) {
            int col = i % cols, row = i / cols;
            int cx = contentX + col * (cardW + gap);
            int cy = startY + row * (cardH + gap);
            ModEntry entry = entries.get(i);
            boolean on = entry.get().getAsBoolean();
            boolean hover = mouseX >= cx && mouseX < cx + cardW && mouseY >= cy && mouseY < cy + cardH;

            RoundedGui.rect(ctx, cx, cy, cardW, cardH, 8, hover ? CARD_BG_HOVER : CARD_BG);

            // "icone": circulo com a letra, so para nao ficar um cartao vazio
            int iconD = 30;
            int iconX = cx + cardW / 2 - iconD / 2;
            int iconY = cy + 14;
            RoundedGui.rect(ctx, iconX, iconY, iconD, iconD, iconD / 2, on ? ACCENT_DIM : 0xFF20242F);
            ctx.drawCenteredTextWithShadow(this.textRenderer, entry.icon(), cx + cardW / 2, iconY + 10, on ? 0xFFCDD6FF : TEXT_DIM);

            int barY = cy + cardH - 22;
            RoundedGui.rect(ctx, cx + 4, barY, cardW - 8, 18, 5, on ? ACCENT : OFF_BAR);
            ctx.drawCenteredTextWithShadow(this.textRenderer, entry.name(), cx + cardW / 2, barY + 5, 0xFFFFFFFF);

            final int fx = cx, fy = cy, fw = cardW, fh = cardH;
            clickAreas.add(new ClickArea(fx, fy, fw, fh, () -> {
                entry.set().accept(!entry.get().getAsBoolean());
                GhostConfig.get().save();
            }));
        }
    }

    // ---------------------------------------------------------------- AMIGOS

    private void renderFriends(DrawContext ctx, int mouseX, int mouseY) {
        int listW = 150;
        int listX = contentX;
        int listY = contentY + 26;
        int listH = contentH - 26 - 26;

        RoundedGui.rect(ctx, listX, listY, listW, listH, 8, CARD_BG);

        List<FriendsClient.Request> incoming = FriendsClient.incoming();
        List<FriendsClient.Friend> friends = FriendsClient.friends();

        int rowY = listY + 6;
        for (FriendsClient.Request req : incoming) {
            RoundedGui.rect(ctx, listX + 4, rowY, listW - 8, 30, 6, 0xFF2A2440);
            ctx.drawText(this.textRenderer, trim(req.username(), 12), listX + 8, rowY + 4, TEXT_MAIN, false);
            ctx.drawText(this.textRenderer, Text.literal("pedido de amizade").formatted(Formatting.ITALIC), listX + 8, rowY + 15, TEXT_DIM, false);
            int ax = listX + listW - 8 - 16;
            final UUID from = req.uuid();
            RoundedGui.rect(ctx, ax, rowY + 5, 16, 20, 4, 0xFF2E7D4F);
            ctx.drawCenteredTextWithShadow(this.textRenderer, "v", ax + 8, rowY + 10, 0xFFFFFFFF);
            clickAreas.add(new ClickArea(ax, rowY + 5, 16, 20, () -> FriendsClient.acceptFriendRequest(from)));
            RoundedGui.rect(ctx, ax - 18, rowY + 5, 16, 20, 4, 0xFF8B2E2E);
            ctx.drawCenteredTextWithShadow(this.textRenderer, "x", ax - 10, rowY + 10, 0xFFFFFFFF);
            clickAreas.add(new ClickArea(ax - 18, rowY + 5, 16, 20, () -> FriendsClient.declineFriendRequest(from)));
            rowY += 34;
        }

        if (friends.isEmpty() && incoming.isEmpty()) {
            ctx.drawText(this.textRenderer, Text.literal("sem amigos ainda").formatted(Formatting.ITALIC), listX + 8, rowY + 4, TEXT_DIM, false);
        }

        for (FriendsClient.Friend f : friends) {
            boolean selected = f.uuid().equals(selectedFriend);
            boolean hover = mouseX >= listX + 4 && mouseX < listX + listW - 4 && mouseY >= rowY && mouseY < rowY + 26;
            if (selected) RoundedGui.rect(ctx, listX + 4, rowY, listW - 8, 26, 6, ACCENT_DIM);
            else if (hover) RoundedGui.rect(ctx, listX + 4, rowY, listW - 8, 26, 6, CARD_BG_HOVER);

            ctx.fill(listX + 10, rowY + 11, listX + 14, rowY + 15, f.online() ? ONLINE : OFFLINE);
            ctx.drawText(this.textRenderer, trim(f.username(), 13), listX + 20, rowY + 9, TEXT_MAIN, false);

            final UUID uuid = f.uuid();
            clickAreas.add(new ClickArea(listX + 4, rowY, listW - 8, 26, () -> selectedFriend = uuid));
            rowY += 28;
        }

        // painel de chat
        int chatX = listX + listW + 12;
        int chatY = listY;
        int chatW = contentX + contentW - chatX;
        int chatH = listH;
        RoundedGui.rect(ctx, chatX, chatY, chatW, chatH, 8, CARD_BG);

        if (selectedFriend == null) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Escolhe um amigo para falar").formatted(Formatting.ITALIC),
                    chatX + chatW / 2, chatY + chatH / 2 - 4, TEXT_DIM);
            return;
        }

        List<FriendsClient.ChatMessage> messages = FriendsClient.chatWith(selectedFriend);
        int msgY = chatY + chatH - 8;
        int maxWidth = chatW - 24;
        // desenha de baixo para cima, a partir das mais recentes
        for (int i = messages.size() - 1; i >= 0 && msgY > chatY + 6; i--) {
            FriendsClient.ChatMessage m = messages.get(i);
            List<String> lines = wrap(m.message(), maxWidth - 16);
            int bubbleH = 6 + lines.size() * 10;
            int bubbleW = Math.min(maxWidth, Math.max(30, lines.stream().mapToInt(this.textRenderer::getWidth).max().orElse(30) + 14));
            int bx = m.mine() ? chatX + chatW - 12 - bubbleW : chatX + 12;
            int by = msgY - bubbleH;
            RoundedGui.rect(ctx, bx, by, bubbleW, bubbleH, 6, m.mine() ? BUBBLE_MINE : BUBBLE_THEIRS);
            int ty = by + 4;
            for (String line : lines) {
                ctx.drawText(this.textRenderer, line, bx + 7, ty, 0xFFF2F3F7, false);
                ty += 10;
            }
            msgY = by - 4;
        }
    }

    // ---------------------------------------------------------------- DEFINICOES

    private void renderSettings(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, Text.literal("Definicoes").formatted(Formatting.BOLD), contentX, contentY, TEXT_MAIN, false);
        ctx.drawText(this.textRenderer, Text.literal("Ghost Client"), contentX, contentY + 20, TEXT_DIM, false);
        ctx.drawText(this.textRenderer, Text.literal("Right Shift abre/fecha este menu."), contentX, contentY + 34, TEXT_DIM, false);
        if (FriendsClient.lastError() != null) {
            ctx.drawText(this.textRenderer, Text.literal("ultimo erro de rede: " + FriendsClient.lastError()).formatted(Formatting.RED),
                    contentX, contentY + 52, 0xFFE06666, false);
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            for (ClickArea area : clickAreas) {
                if (area.hit(click.x(), click.y())) {
                    area.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
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
        if (s.length() > max) s = s.substring(0, max - 1) + "..";
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
