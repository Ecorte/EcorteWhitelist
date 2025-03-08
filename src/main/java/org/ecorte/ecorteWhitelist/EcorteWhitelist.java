package org.ecorte.ecorteWhitelist;

import dev.jorel.commandapi.arguments.BooleanArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class EcorteWhitelist extends JavaPlugin {
    public Config config;
    public final Set<UUID> whitelist = Collections.synchronizedSet(new HashSet<>());
    public DBManager dbManager;

    @Override
    public void onLoad() {
        // Plugin startup logic
        CommandAPICommand AddCommand = new CommandAPICommand("add")
                .withPermission("ecortewhitelist.whitelist.add")
                .withArguments(new OfflinePlayerArgument("player"))
                .executes((sender, args) -> {
                    OfflinePlayer player = (OfflinePlayer) args.get("player");
                    var mm = MiniMessage.miniMessage();
                    Component parsed;
                    if (player == null) {
                        parsed = mm.deserialize(this.config.responsePrefix + " Player not found");
                        sender.sendMessage(parsed);
                        return;
                    }
                    UUID uuid = player.getUniqueId();
                    UUID senderUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
                    if (sender instanceof Player) senderUUID = ((Player) sender).getUniqueId();
                    this.AddToWhitelist(uuid, senderUUID);
                    this.getComponentLogger().info(Component.text("Player " + player.getName() + " added to the whitelist by " + sender.getName()));
                    parsed = mm.deserialize(this.config.responsePrefix + " Player <green>" + player.getName() + "</green> added to the whitelist by <gold>" + sender.getName() + "</gold>");
                    sender.sendMessage(parsed);
                });

        CommandAPICommand RemoveCommand = new CommandAPICommand("remove")
                .withPermission("ecortewhitelist.whitelist.remove")
                .withArguments(new OfflinePlayerArgument("player"))
                .executes((sender, args) -> {
                    OfflinePlayer player = (OfflinePlayer) args.get("player");
                    var mm = MiniMessage.miniMessage();
                    Component parsed;
                    if (player == null) {
                        parsed = mm.deserialize(this.config.responsePrefix + " Player not found");
                        sender.sendMessage(parsed);
                        return;
                    }
                    UUID uuid = player.getUniqueId();
                    this.RemoveFromWhitelist(uuid);
                    parsed = mm.deserialize(this.config.responsePrefix + " Player <green>" + player.getName() + "</green> removed from the whitelist");
                    sender.sendMessage(parsed);
                });

        CommandAPICommand StatusCommand = new CommandAPICommand("status")
                .withPermission("ecortewhitelist.whitelist.status")
                .withArguments(new OfflinePlayerArgument("player"))
                .executes((sender, args) -> {
                    OfflinePlayer player = (OfflinePlayer) args.get("player");
                    var mm = MiniMessage.miniMessage();
                    Component parsed;
                    if (player == null) {
                        parsed = mm.deserialize(this.config.responsePrefix + " Player not found");
                        sender.sendMessage(parsed);
                        return;
                    }
                    UUID uuid = player.getUniqueId();
                    if (this.whitelist.contains(uuid)) {
                        parsed = mm.deserialize(this.config.responsePrefix + " Player <green>" + player.getName() + "</green> is whitelisted");
                    } else {
                        parsed = mm.deserialize(this.config.responsePrefix + " Player <green>" + player.getName() + "</green> is not whitelisted");
                    }
                    sender.sendMessage(parsed);
                });

        CommandAPICommand ToggleCommand = new CommandAPICommand("toggle")
                .withPermission("ecortewhitelist.toggle")
                .withOptionalArguments(new BooleanArgument("state"))
                .executes((sender, args) -> {
                    var mm = MiniMessage.miniMessage();
                    boolean oldState = this.config.requireWhitelist;
                    boolean newState;
                    if (args.get("state") == null) {
                        newState = !oldState;
                    } else {
                        newState = (boolean) args.get("state");
                    }
                    if (newState == oldState) {
                        sender.sendMessage(mm.deserialize(this.config.responsePrefix + " The whitelist requirement is already " + newState));
                        return;
                    }

                    this.config.requireWhitelist = newState;

                    sender.sendMessage(mm.deserialize(this.config.responsePrefix + " Toggled the whitelist requirement to " + newState));
                });

        new CommandAPICommand("ecortewl")
                .withAliases("ecortewhitelist", "ewl")
                .withSubcommand(AddCommand)
                .withSubcommand(RemoveCommand)
                .withSubcommand(StatusCommand)
                .withSubcommand(ToggleCommand)
                .register(this);

    }

    public void LoadWhitelist() {
        if (this.dbManager.ds == null) {
            System.err.println("Database connection is not initialized.");
            return;
        }

        String query = "SELECT uuid FROM ecortewhitelist_whitelist";

        try (Connection connection = this.dbManager.ds.getConnection();
             PreparedStatement selectStmt = connection.prepareStatement(query);
             ResultSet resultSet = selectStmt.executeQuery()) {
            this.whitelist.clear();

            while (resultSet.next()) {
                String uuidString = resultSet.getString("uuid");
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    this.whitelist.add(uuid);
                } catch (IllegalArgumentException e) {
                    this.getComponentLogger().warn(Component.text("Invalid UUID format in database: " + uuidString));
                }
            }

        } catch (SQLException e) {
            this.getComponentLogger().error(Component.text("Error loading whitelist from the database:"), e);
        }
    }

    private void AddToWhitelist(UUID uuid, UUID author) {
        if (this.dbManager.ds == null) {
            this.getComponentLogger().error(Component.text("Database connection is not initialized."));
            return;
        }

        String query = "INSERT INTO ecortewhitelist_whitelist (uuid, author) VALUES (?, ?)";

        try (Connection conn = this.dbManager.ds.getConnection();
             PreparedStatement insertStmt = conn.prepareStatement(query)) {
            insertStmt.setString(1, uuid.toString());
            insertStmt.setString(2, author.toString());
            insertStmt.executeUpdate();
        } catch (SQLException e) {
            this.getComponentLogger().error(Component.text("Error adding UUID to the whitelist:"), e);
        }
        this.LoadWhitelist();
    }

    private void RemoveFromWhitelist(UUID uuid) {
        if (this.dbManager.ds == null) {
            this.getComponentLogger().error(Component.text("Database connection is not initialized."));
            return;
        }

        String query = "DELETE FROM ecortewhitelist_whitelist WHERE uuid = ?";

        try (Connection connection = this.dbManager.ds.getConnection();
                PreparedStatement deleteStmt = connection.prepareStatement(query)) {
            deleteStmt.setString(1, uuid.toString());
            deleteStmt.executeUpdate();
        } catch (SQLException e) {
            this.getComponentLogger().error(Component.text("Error removing UUID from the whitelist:"), e);
        }
        this.LoadWhitelist();
    }

    @Override
    public void onEnable() {
        int pluginId = 24211;
        Metrics metrics = new Metrics(this, pluginId);

        // Plugin startup logic
        this.config = Config.getInstance(this);
        this.dbManager = DBManager.getInstance(this);
        this.dbManager.connectToSQL();
        LoadWhitelist();

        // Listeners
        getServer().getPluginManager().registerEvents(new WhitelistListener(this), this);

        // LuckPerms
        LuckPerms luckPerms = getServer().getServicesManager().load(LuckPerms.class);
        if (luckPerms != null) {
            ContextManager contextManager = luckPerms.getContextManager();
            contextManager.registerCalculator(new WhitelistContext(this));
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.dbManager.close();
    }
}
