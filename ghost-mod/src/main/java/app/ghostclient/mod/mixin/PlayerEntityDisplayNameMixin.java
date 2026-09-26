package app.ghostclient.mod.mixin;

import app.ghostclient.mod.GhostPlayers;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Logo do Ghost a esquerda do nome por cima da cabeca dos jogadores. */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityDisplayNameMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void ghostclient$badge(CallbackInfoReturnable<Text> cir) {
        Object self = this;
        if (self instanceof AbstractClientPlayerEntity player) {
            cir.setReturnValue(GhostPlayers.withBadge(GhostPlayers.idFor(player), cir.getReturnValue()));
        }
    }
}
