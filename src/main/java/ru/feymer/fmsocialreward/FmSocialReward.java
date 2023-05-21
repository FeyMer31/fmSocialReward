package ru.feymer.fmsocialreward;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class FmSocialReward extends JavaPlugin implements Listener {

    Connection con;
    Statement stmt;
    ResultSet rs;
    public File file = new File("plugins/fmSocialReward", "config.yml");
    public static FileConfiguration config;

    public FmSocialReward() {
        this.config = YamlConfiguration.loadConfiguration(this.file);
    }

    @Override
    public void onEnable() {
        getLogger().info("Плагин включен!");
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        this.checkPlayer("TestNicknameForCheckDatabaseConnection");

    }

    public boolean checkPlayer(String name) {
        String url = "jdbc:mysql://" + this.getConfig().getString("mysql.ip") + ":" + this.getConfig().getString("mysql.port") + "/" + this.getConfig().getString("mysql.database");
        String user = this.getConfig().getString("mysql.user");
        String password = this.getConfig().getString("mysql.password");
        String query = "select " + this.getConfig().getString("mysql.players-field") + " from " + this.getConfig().getString("mysql.table")+" where lower(" +this.getConfig().getString("mysql.players-field") +")='"+name.toLowerCase()+"'";

        try {
            try {
                this.con = DriverManager.getConnection(url, user, password);
                this.stmt = this.con.createStatement();
                this.rs = this.stmt.executeQuery(query);
                if (!this.rs.next()) {
                    return false;
                }

                String count = this.rs.getString(1);
                if (count.equals(name)) {
                    return true;
                }
            } catch (SQLException nocon) {
                getLogger().fine("Не могу подключится к MySQL");
                Bukkit.getPluginManager().disablePlugin(this);
                nocon.printStackTrace();
                return false;
            }
        } finally {
            try {
                this.con.close();
            } catch (SQLException closecon) {
            }

            try {
                this.stmt.close();
            } catch (SQLException closestmt) {
            }

            try {
                this.rs.close();
            } catch (SQLException closers) {
            }

        }
        return true;
    }

    @EventHandler
    public void Reward(PlayerCommandPreprocessEvent e) {
        if (e.getMessage().equalsIgnoreCase("/socialreward")) {
            if (this.checkPlayer(e.getPlayer().getName())) {
                if (!config.contains("player-" + e.getPlayer().getName())) {
                    e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("messages.reward.reward_ok")));
                    this.getConfig().getStringList("give-reward.commands").forEach((cmd) -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ChatColor.translateAlternateColorCodes('&', cmd.replace("%player%", e.getPlayer().getName())));
                    });
                    for (String ss : getConfig().getStringList("give-reward.broadcast-reward")) {
                        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', ss).replace("%player%", e.getPlayer().getName()));
                    }
                    config.set("player-" + e.getPlayer().getName(), "received");
                    try {
                        config.save(file);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    e.setCancelled(true);
                } else {
                    e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("messages.reward.reward_received")));
                    e.setCancelled(true);
                }
            } else {
                e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("messages.reward.reward_null")));
                e.setCancelled(true);
            }
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Плагин выключен!");
    }
}
