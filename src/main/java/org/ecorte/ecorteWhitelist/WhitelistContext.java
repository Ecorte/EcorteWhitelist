package org.ecorte.ecorteWhitelist;

import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WhitelistContext implements ContextCalculator<Player> {
    private final EcorteWhitelist plugin;
    public WhitelistContext(EcorteWhitelist plugin) {
        this.plugin = plugin;
    }

    @Override
    public void calculate(Player target, ContextConsumer consumer) {
        consumer.accept("isWhitelisted", this.plugin.whitelist.contains(target.getUniqueId()) ? "true" : "false");
    }

    @Override
    public @NotNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
        builder.add("isWhitelisted", "false");
        builder.add("isWhitelisted", "true");
        return builder.build();
    }
}
