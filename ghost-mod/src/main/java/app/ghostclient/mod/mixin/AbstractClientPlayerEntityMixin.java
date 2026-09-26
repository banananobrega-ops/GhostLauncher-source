package app.ghostclient.mod.mixin;

import app.ghostclient.mod.GhostPlayers;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Troca a capa do jogador pela capa ativa dele no Ghost Client (so do lado do cliente). */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void ghostclient$cape(CallbackInfoReturnable<SkinTextures> cir) {
        Identifier cape = GhostPlayers.capeFor(GhostPlayers.idFor((AbstractClientPlayerEntity) (Object) this));
        if (cape != null) {
            cir.setReturnValue(GhostPlayers.withCape(cir.getReturnValue(), cape));
        }
    }
}
