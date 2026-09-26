package app.ghostclient.mod.mixin;

import app.ghostclient.mod.gui.GhostMenuScreen;
import app.ghostclient.mod.gui.GhostMenuScreenEnhanced;
import app.ghostclient.mod.gui.GhostMenuScreenPurple;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Right Shift liga/desliga o menu do Ghost Client (toggle), tal como pedido.
 * Nao usa Fabric API de propósito (o mod nao depende dela): le a tecla direto do
 * GLFW a cada tick do cliente, com deteccao de "acabou de ser premida" para nao
 * abrir/fechar em loop enquanto a tecla fica em baixo.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientKeyMixin {

    @Unique
    private boolean ghostclient$wasDown = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void ghostclient$toggleMenu(CallbackInfo ci) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        if (mc.getWindow() == null) return;

        boolean down = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (down && !ghostclient$wasDown) {
            Screen current = mc.currentScreen;
            // Support all menu versions
            if (current instanceof GhostMenuScreen || current instanceof GhostMenuScreenEnhanced || current instanceof GhostMenuScreenPurple) {
                mc.setScreen(null);
            } else if (current == null) {
                // Use the new purple menu by default
                mc.setScreen(new GhostMenuScreenPurple());
            }
            // se outro ecra qualquer estiver aberto (inventario, chat, menu de servidor...)
            // nao mexe, para nao atrapalhar o resto do jogo.
        }
        ghostclient$wasDown = down;
    }
}
