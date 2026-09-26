package app.ghostclient.mod;

import net.fabricmc.api.ClientModInitializer;

public class GhostClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GhostPlayers.start();
        FriendsClient.start();
    }
}
