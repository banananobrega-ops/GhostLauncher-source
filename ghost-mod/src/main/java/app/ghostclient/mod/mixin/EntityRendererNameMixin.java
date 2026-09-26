package app.ghostclient.mod.mixin;

import app.ghostclient.mod.GhostPlayers;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * O texto do nametag (por cima da cabeca) sai daqui. Assim a logo aparece mesmo que o
 * getDisplayName() do jogador nao seja o caminho usado nesta versao.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererNameMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void ghostclient$badge(Entity entity, CallbackInfoReturnable<Text> cir) {
        if (entity instanceof AbstractClientPlayerEntity player) {
            cir.setReturnValue(GhostPlayers.withBadge(GhostPlayers.idFor(player), cir.getReturnValue()));
        }
    }
}
