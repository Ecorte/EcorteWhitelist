package org.ecorte.ecorteWhitelist;

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
    public LuckPerms luckPerms;
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
                    if (player == null) {
                        sender.sendMessage("Player not found");
                        return;
                    }
                    UUID uuid = player.getUniqueId();
                    UUID senderUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
                    if (sender instanceof Player) senderUUID = ((Player) sender).getUniqueId();
                    this.AddToWhitelist(uuid, senderUUID);
                    sender.sendMessage("Player added");
                });

        CommandAPICommand RemoveCommand = new CommandAPICommand("remove")
                .withPermission("ecortewhitelist.whitelist.remove")
                .withArguments(new OfflinePlayerArgument("player"))
                .executes((sender, args) -> {
                    OfflinePlayer player = (OfflinePlayer) args.get("player");
                    if (player == null) {
                        sender.sendMessage("Player not found");
                        return;
                    }
                    UUID uuid = player.getUniqueId();
                    this.RemoveFromWhitelist(uuid);
                    sender.sendMessage("UUID removed");
                });

        CommandAPICommand StatusCommand = new CommandAPICommand("status")
                .withPermission("ecortewhitelist.whitelist.status")
                .withArguments(new OfflinePlayerArgument("player"))
                .executes((sender, args) -> {
                    OfflinePlayer player = (OfflinePlayer) args.get("player");
                    if (player == null) {
                        sender.sendMessage("Player not found");
                        return;
                    }
                    UUID uuid = player.getUniqueId();
                    if (this.whitelist.contains(uuid)) {
                        sender.sendMessage("UUID is whitelisted");
                    } else {
                        sender.sendMessage("UUID is not whitelisted");
                    }
                });

        new CommandAPICommand("ecortewl")
                .withAliases("ecortewhitelist", "ewl")
                .withSubcommand(AddCommand)
                .withSubcommand(RemoveCommand)
                .withSubcommand(StatusCommand)
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
                    getLogger().severe("Invalid UUID format in database: " + uuidString);
                }
            }

        } catch (SQLException e) {
            getLogger().severe("Error loading whitelist from the database:");
            e.printStackTrace();
        }
    }

    private void AddToWhitelist(UUID uuid, UUID author) {
        if (this.dbManager.ds == null) {
            getLogger().severe("Database connection is not initialized.");
            return;
        }

        String query = "INSERT INTO ecortewhitelist_whitelist (uuid, author) VALUES (?, ?)";

        try (Connection conn = this.dbManager.ds.getConnection();
             PreparedStatement insertStmt = conn.prepareStatement(query)) {
            insertStmt.setString(1, uuid.toString());
            insertStmt.setString(2, author.toString());
            insertStmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("Error adding UUID to the whitelist:");
            e.printStackTrace();
        }
        this.LoadWhitelist();
    }

    private void RemoveFromWhitelist(UUID uuid) {
        if (this.dbManager.ds == null) {
            getLogger().severe("Database connection is not initialized.");
            return;
        }

        String query = "DELETE FROM ecortewhitelist_whitelist WHERE uuid = ?";

        try (Connection connection = this.dbManager.ds.getConnection();
                PreparedStatement deleteStmt = connection.prepareStatement(query)) {
            deleteStmt.setString(1, uuid.toString());
            deleteStmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("Error removing UUID from the whitelist:");
            e.printStackTrace();
        }
        this.LoadWhitelist();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.config = Config.getInstance(this);
        this.dbManager = DBManager.getInstance(this);
        this.dbManager.connectToSQL();
        LoadWhitelist();

        // Listeners
        getServer().getPluginManager().registerEvents(new WhitelistListener(this), this);

        // LuckPerms
        this.luckPerms = getServer().getServicesManager().load(LuckPerms.class);
        if (this.luckPerms == null) {
            throw new IllegalStateException("LuckPerms API not loaded.");
        }
        ContextManager contextManager = this.luckPerms.getContextManager();
        contextManager.registerCalculator(new WhitelistContext(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.dbManager.close();
    }
}
