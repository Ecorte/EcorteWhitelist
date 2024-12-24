package org.ecorte.ecorteWhitelist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class WhitelistListener implements Listener {

    public EcorteWhitelist plugin;

    public WhitelistListener(EcorteWhitelist plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.config.requireWhitelist) {
            return;
        }

        Player player = event.getPlayer();

        if (player.isOp() || player.hasPermission("ecortewhitelist.whitelist.bypass")) {
            return;
        }

        if (!plugin.whitelist.contains(event.getPlayer().getUniqueId())) {
            event.joinMessage(null);

            MiniMessage mm = MiniMessage.miniMessage();
            Component parsed = mm.deserialize(plugin.config.kickMessage);
            event.getPlayer().kick(parsed);
        }
    }
}
