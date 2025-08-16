package com.playerMarket_Sig;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * 配置管理类
 * By sigmoid QQ 1219269148
 */
public class ConfigManager {

    private final PlayerMarket_Sig plugin;
    private FileConfiguration config;

    public ConfigManager(PlayerMarket_Sig plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public double getTaxRate() {
        return config.getDouble("tax-rate", 0.3);
    }

    public int getMaxSellItems() {
        return config.getInt("max-sell-items", 10);
    }

    public String getSellTitle() {
        return config.getString("gui.sell-title", "§6玩家寄卖 - 上传物品");
    }

    public String getMarketTitle() {
        return config.getString("gui.market-title", "§6玩家市场 - 购买物品");
    }

    public String getMessage(String key) {
        return config.getString("messages." + key, "§c消息配置错误: " + key);
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
}