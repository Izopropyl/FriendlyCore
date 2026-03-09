package com.friendlysmp.core.features.withersound;

import com.friendlysmp.core.storage.PlayerSettingsStore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class WitherSoundJoinListener implements Listener {
    private final PlayerSettingsStore store;

    public WitherSoundJoinListener(PlayerSettingsStore store) {
        this.store = store;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        store.ensureLoadedAsync(e.getPlayer().getUniqueId());
    }
}