package app.ghostclient.mod.mixin;

import app.ghostclient.mod.GhostPlayers;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Logo do Ghost a esquerda do nome na lista de jogadores (Tab). */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void ghostclient$badge(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(GhostPlayers.withBadge(entry.getProfile().id(), cir.getReturnValue()));
    }
}
