package com.playerMarket_Sig;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PlayerMarket_Sig - 玩家市场插件
 * By sigmoid QQ 1219269148
 */
public class PlayerMarket_Sig extends JavaPlugin {

    private static PlayerMarket_Sig instance;
    private static Economy economy = null;
    private ConfigManager configManager;
    private MarketData marketData;
    private MarketGUI marketGUI;

    @Override
    public void onEnable() {
        instance = this;

        // 检查Vault依赖
        if (!setupEconomy()) {
            getLogger().severe("未找到Vault插件，插件已禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 初始化配置
        configManager = new ConfigManager(this);

        // 初始化数据存储
        marketData = new MarketData(this);
        marketData.loadData();

        // 初始化GUI
        marketGUI = new MarketGUI(this);

        // 注册命令
        new CommandHandler(this);

        getLogger().info("PlayerMarket_Sig 插件已启用！By sigmoid QQ 1219269148");
    }

    @Override
    public void onDisable() {
        if (marketData != null) {
            marketData.saveData();
        }
        getLogger().info("PlayerMarket_Sig 插件已禁用！");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return economy != null;
    }

    public static PlayerMarket_Sig getInstance() {
        return instance;
    }

    public static Economy getEconomy() {
        return economy;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MarketData getMarketData() {
        return marketData;
    }

    public MarketGUI getMarketGUI() {
        return marketGUI;
    }
}