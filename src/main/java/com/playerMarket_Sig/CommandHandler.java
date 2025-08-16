package com.playerMarket_Sig;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 命令处理类
 * By sigmoid QQ 1219269148
 */
public class CommandHandler implements CommandExecutor {

    private final PlayerMarket_Sig plugin;

    public CommandHandler(PlayerMarket_Sig plugin) {
        this.plugin = plugin;
        plugin.getCommand("pma").setExecutor(this);
        plugin.getCommand("pmb").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }

        if (!player.hasPermission("playermarket.use")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "pma" -> plugin.getMarketGUI().openSellGUI(player);
            case "pmb" -> plugin.getMarketGUI().openMarketGUI(player, 0);
        }

        return true;
    }
}