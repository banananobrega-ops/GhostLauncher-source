package app.ghostclient.mod.mixin;

import app.ghostclient.mod.GhostPlayers;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Igual ao PlayerEntityDisplayNameMixin, para o caso de a classe do jogador nao ter o
 * seu proprio getDisplayName nesta versao (o badge nunca e posto duas vezes).
 */
@Mixin(Entity.class)
public abstract class EntityDisplayNameMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void ghostclient$badge(CallbackInfoReturnable<Text> cir) {
        Object self = this;
        if (self instanceof AbstractClientPlayerEntity player) {
            cir.setReturnValue(GhostPlayers.withBadge(GhostPlayers.idFor(player), cir.getReturnValue()));
        }
    }
}
