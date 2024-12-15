package org.ecorte.ecorteWhitelist;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class WhitelistListener implements Listener {

    public EcorteWhitelist plugin;

    public WhitelistListener(EcorteWhitelist plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.config.requireWhitelist) {
            return;
        }

        Player player = event.getPlayer();

        if (player.isOp() || player.hasPermission("ecortewhitelist.whitelist.bypass")) {
            return;
        }

        if (!plugin.whitelist.contains(event.getPlayer().getUniqueId())) {
            final Component component = Component.text("You are not on the whitelist!");
            event.getPlayer().kick(component);
        }
    }
}
