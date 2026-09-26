package app.ghostclient.mod.mixin;

import app.ghostclient.mod.gui.GhostMenuScreen;
import app.ghostclient.mod.gui.RoundedGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Da uma cara ao menu de pausa (tecla ESC): um banner com o nome do Ghost Client
 * la em cima e um botao de atalho para o menu do mod. Nao mexe nos botoes
 * originais (Opcoes, Sair, etc.) de proposito -- assim o resto do menu de pausa
 * continua sempre a funcionar tal e qual, mesmo que a lista de botoes do vanilla
 * mude de versao para versao.
 *
 * Usa o truque de "extends Screen" para poder aceder aos metodos/campos
 * protegidos herdados (addDrawableChild, width, height...). O construtor abaixo
 * nunca corre a serio -- e so para o compilador aceitar o "extends".
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void ghostclient$addButton(CallbackInfo ci) {
        int w = 100, h = 20;
        int x = this.width - w - 8;
        int y = 8;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Ghost Client"), b -> {
            MinecraftClient.getInstance().setScreen(new GhostMenuScreen());
        }).dimensions(x, y, w, h).build());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void ghostclient$banner(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int bannerY = 26;
        int bannerH = 24;
        RoundedGui.rect(ctx, this.width / 2 - 120, bannerY, 240, bannerH, 8, 0xB0141621);
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawCenteredTextWithShadow(tr, Text.literal("GHOST CLIENT").formatted(Formatting.BOLD),
                this.width / 2, bannerY + 7, 0xFF7C97FF);
    }
}
