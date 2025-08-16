package com.playerMarket_Sig;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 市场数据存储类
 * By sigmoid QQ 1219269148
 */
public class MarketData {

    private final PlayerMarket_Sig plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, MarketItem> marketItems = new HashMap<>();
    private final Map<UUID, List<UUID>> playerItems = new HashMap<>();

    public MarketData(PlayerMarket_Sig plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "market-data.yml");
        loadData();
    }

    public void loadData() {
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建数据文件: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // 加载市场物品
        if (dataConfig.contains("market-items")) {
            for (String key : dataConfig.getConfigurationSection("market-items").getKeys(false)) {
                try {
                    UUID itemId = UUID.fromString(key);
                    String sellerName = dataConfig.getString("market-items." + key + ".seller");
                    UUID sellerId = UUID.fromString(dataConfig.getString("market-items." + key + ".seller-uuid"));
                    double price = dataConfig.getDouble("market-items." + key + ".price");
                    long timestamp = dataConfig.getLong("market-items." + key + ".timestamp");
                    ItemStack item = dataConfig.getItemStack("market-items." + key + ".item");

                    if (item != null) {
                        MarketItem marketItem = new MarketItem(itemId, sellerId, sellerName, item, price, timestamp);
                        marketItems.put(itemId, marketItem);

                        // 更新玩家物品列表
                        playerItems.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(itemId);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("加载市场物品数据时出错: " + e.getMessage());
                }
            }
        }
    }

    public void saveData() {
        for (Map.Entry<UUID, MarketItem> entry : marketItems.entrySet()) {
            UUID itemId = entry.getKey();
            MarketItem item = entry.getValue();
            String path = "market-items." + itemId.toString();

            dataConfig.set(path + ".seller", item.getSellerName());
            dataConfig.set(path + ".seller-uuid", item.getSellerId().toString());
            dataConfig.set(path + ".price", item.getPrice());
            dataConfig.set(path + ".timestamp", item.getTimestamp());
            dataConfig.set(path + ".item", item.getItem());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存数据文件时出错: " + e.getMessage());
        }
    }

    public void addMarketItem(UUID sellerId, String sellerName, ItemStack item, double price) {
        UUID itemId = UUID.randomUUID();
        MarketItem marketItem = new MarketItem(itemId, sellerId, sellerName, item, price, System.currentTimeMillis());
        marketItems.put(itemId, marketItem);
        playerItems.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(itemId);
        saveData();
    }

    public void removeMarketItem(UUID itemId) {
        MarketItem item = marketItems.remove(itemId);
        if (item != null) {
            List<UUID> playerItemList = playerItems.get(item.getSellerId());
            if (playerItemList != null) {
                playerItemList.remove(itemId);
                if (playerItemList.isEmpty()) {
                    playerItems.remove(item.getSellerId());
                }
            }

            // 从配置文件中移除
            dataConfig.set("market-items." + itemId.toString(), null);
            saveData();
        }
    }

    public MarketItem getMarketItem(UUID itemId) {
        return marketItems.get(itemId);
    }

    public List<MarketItem> getAllMarketItems() {
        return new ArrayList<>(marketItems.values());
    }

    public List<MarketItem> getPlayerMarketItems(UUID playerId) {
        List<UUID> itemIds = playerItems.get(playerId);
        if (itemIds == null) return new ArrayList<>();

        return itemIds.stream()
                .map(marketItems::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public int getPlayerItemCount(UUID playerId) {
        return playerItems.getOrDefault(playerId, new ArrayList<>()).size();
    }

    public static class MarketItem {
        private final UUID itemId;
        private final UUID sellerId;
        private final String sellerName;
        private final ItemStack item;
        private final double price;
        private final long timestamp;

        public MarketItem(UUID itemId, UUID sellerId, String sellerName, ItemStack item, double price, long timestamp) {
            this.itemId = itemId;
            this.sellerId = sellerId;
            this.sellerName = sellerName;
            this.item = item.clone();
            this.price = price;
            this.timestamp = timestamp;
        }

        public UUID getItemId() { return itemId; }
        public UUID getSellerId() { return sellerId; }
        public String getSellerName() { return sellerName; }
        public ItemStack getItem() { return item.clone(); }
        public double getPrice() { return price; }
        public long getTimestamp() { return timestamp; }
    }
}