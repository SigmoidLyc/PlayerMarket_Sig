package com.playerMarket_Sig;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI界面管理类
 * By sigmoid QQ 1219269148
 */
public class MarketGUI implements Listener {

    private final PlayerMarket_Sig plugin;
    private final Map<UUID, InputState> playerInputStates = new HashMap<>();
    private final Map<UUID, ItemStack> tempItems = new HashMap<>();
    private final Map<UUID, Double> tempPrices = new HashMap<>();
    private final Map<UUID, Integer> marketPages = new HashMap<>();

    enum InputState {
        WAITING_PRICE,
        WAITING_AMOUNT
    }

    public MarketGUI(PlayerMarket_Sig plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openSellGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, plugin.getConfigManager().getSellTitle());

        // 显示玩家当前上架的物品
        List<MarketData.MarketItem> playerItems = plugin.getMarketData().getPlayerMarketItems(player.getUniqueId());
        int slot = 0;
        for (MarketData.MarketItem marketItem : playerItems) {
            if (slot >= 45) break; // 保留最后一行给功能按钮

            ItemStack displayItem = marketItem.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore();
                if (lore == null) lore = new ArrayList<>();
                lore.add("§6单价: §e" + marketItem.getPrice());
                lore.add("§6数量: §e" + displayItem.getAmount());
                lore.add("§6总价: §e" + (marketItem.getPrice() * displayItem.getAmount()));
                lore.add("§7点击下架");
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            gui.setItem(slot++, displayItem);
        }

        // 上架物品按钮
        ItemStack sellButton = new ItemStack(Material.EMERALD);
        ItemMeta sellMeta = sellButton.getItemMeta();
        sellMeta.setDisplayName("§a上架手中物品");
        sellMeta.setLore(Arrays.asList(
                "§7手持物品点击此按钮",
                "§7① 先输入单个物品的价格",
                "§7② 再输入要上架的数量",
                "§7③ 从背包扣除指定数量",
                "§e当前上架数量: " + playerItems.size() + "/" + plugin.getConfigManager().getMaxSellItems()
        ));
        sellButton.setItemMeta(sellMeta);
        gui.setItem(45, sellButton);

        // 进入市场按钮
        ItemStack marketButton = new ItemStack(Material.CHEST);
        ItemMeta marketMeta = marketButton.getItemMeta();
        marketMeta.setDisplayName("§6进入玩家市场");
        marketMeta.setLore(Arrays.asList("§7点击查看所有玩家的商品"));
        marketButton.setItemMeta(marketMeta);
        gui.setItem(49, marketButton);

        // 关闭按钮
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName("§c关闭界面");
        closeButton.setItemMeta(closeMeta);
        gui.setItem(53, closeButton);

        player.openInventory(gui);
    }

    public void openMarketGUI(Player player, int page) {
        List<MarketData.MarketItem> allItems = plugin.getMarketData().getAllMarketItems();
        int itemsPerPage = 45; // 前45个槽位放物品
        int totalPages = (int) Math.ceil((double) allItems.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        // 确保页码在有效范围内
        page = Math.max(0, Math.min(page, totalPages - 1));
        marketPages.put(player.getUniqueId(), page);

        Inventory gui = Bukkit.createInventory(null, 54, plugin.getConfigManager().getMarketTitle() + " §7(" + (page + 1) + "/" + totalPages + ")");

        // 显示当前页的物品
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            MarketData.MarketItem marketItem = allItems.get(i);
            int slot = i - startIndex;

            ItemStack displayItem = marketItem.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore();
                if (lore == null) lore = new ArrayList<>();
                double totalPrice = marketItem.getPrice() * displayItem.getAmount();
                lore.add("§6单价: §e" + marketItem.getPrice());
                lore.add("§6数量: §e" + displayItem.getAmount());
                lore.add("§6总价: §e" + totalPrice);
                lore.add("§7卖家: §e" + marketItem.getSellerName());
                if (!marketItem.getSellerId().equals(player.getUniqueId())) {
                    lore.add("§a点击购买");
                    lore.add("§c§l点击花费 " + totalPrice + " 购买该槽位的全部物品，不可逆转！");
                } else {
                    lore.add("§c这是你的物品");
                }
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            gui.setItem(slot, displayItem);
        }

        // 返回寄卖界面按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§e返回寄卖界面");
        backButton.setItemMeta(backMeta);
        gui.setItem(45, backButton);

        // 上一页按钮
        if (page > 0) {
            ItemStack prevButton = new ItemStack(Material.SPECTRAL_ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            prevMeta.setDisplayName("§a上一页");
            prevMeta.setLore(Arrays.asList("§7当前: 第" + (page + 1) + "页", "§7点击前往第" + page + "页"));
            prevButton.setItemMeta(prevMeta);
            gui.setItem(48, prevButton);
        }

        // 下一页按钮
        if (page < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.TIPPED_ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            nextMeta.setDisplayName("§a下一页");
            nextMeta.setLore(Arrays.asList("§7当前: 第" + (page + 1) + "页", "§7点击前往第" + (page + 2) + "页"));
            nextButton.setItemMeta(nextMeta);
            gui.setItem(50, nextButton);
        }

        // 关闭按钮
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName("§c关闭界面");
        closeButton.setItemMeta(closeMeta);
        gui.setItem(53, closeButton);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!title.equals(plugin.getConfigManager().getSellTitle()) &&
                !title.contains(plugin.getConfigManager().getMarketTitle())) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();

        if (title.equals(plugin.getConfigManager().getSellTitle())) {
            handleSellGUIClick(player, clicked, event.getSlot());
        } else if (title.contains(plugin.getConfigManager().getMarketTitle())) {
            handleMarketGUIClick(player, clicked, event.getSlot());
        }
    }

    private void handleSellGUIClick(Player player, ItemStack clicked, int slot) {
        if (slot == 45 && clicked.getType() == Material.EMERALD) {
            // 上架物品
            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem.getType() == Material.AIR) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-item"));
                return;
            }

            int currentCount = plugin.getMarketData().getPlayerItemCount(player.getUniqueId());
            if (currentCount >= plugin.getConfigManager().getMaxSellItems()) {
                player.sendMessage(plugin.getConfigManager().getMessage("max-items-reached")
                        .replace("%max%", String.valueOf(plugin.getConfigManager().getMaxSellItems())));
                return;
            }

            UUID playerId = player.getUniqueId();
            tempItems.put(playerId, handItem.clone());
            playerInputStates.put(playerId, InputState.WAITING_PRICE);

            player.closeInventory();
            player.sendMessage("§a请在聊天栏输入物品单价 (输入 'cancel' 取消):");
            player.sendMessage("§7手中物品: §e" + getItemDisplayName(handItem) + " §7x" + handItem.getAmount());

        } else if (slot == 49 && clicked.getType() == Material.CHEST) {
            // 进入市场
            openMarketGUI(player, 0);

        } else if (slot == 53 && clicked.getType() == Material.BARRIER) {
            // 关闭界面
            player.closeInventory();

        } else if (slot < 45) {
            // 下架物品
            List<MarketData.MarketItem> playerItems = plugin.getMarketData().getPlayerMarketItems(player.getUniqueId());
            if (slot < playerItems.size()) {
                MarketData.MarketItem item = playerItems.get(slot);
                plugin.getMarketData().removeMarketItem(item.getItemId());

                // 返还物品
                player.getInventory().addItem(item.getItem());
                player.sendMessage(plugin.getConfigManager().getMessage("item-removed")
                        .replace("%item%", getItemDisplayName(item.getItem())));

                // 刷新界面
                openSellGUI(player);
            }
        }
    }

    private void handleMarketGUIClick(Player player, ItemStack clicked, int slot) {
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            // 返回寄卖界面
            openSellGUI(player);

        } else if (slot == 48 && clicked.getType() == Material.SPECTRAL_ARROW) {
            // 上一页
            int currentPage = marketPages.getOrDefault(player.getUniqueId(), 0);
            openMarketGUI(player, currentPage - 1);

        } else if (slot == 50 && clicked.getType() == Material.TIPPED_ARROW) {
            // 下一页
            int currentPage = marketPages.getOrDefault(player.getUniqueId(), 0);
            openMarketGUI(player, currentPage + 1);

        } else if (slot == 53 && clicked.getType() == Material.BARRIER) {
            // 关闭界面
            player.closeInventory();

        } else if (slot < 45) {
            // 购买物品
            List<MarketData.MarketItem> allItems = plugin.getMarketData().getAllMarketItems();
            int currentPage = marketPages.getOrDefault(player.getUniqueId(), 0);
            int itemIndex = currentPage * 45 + slot;

            if (itemIndex < allItems.size()) {
                MarketData.MarketItem item = allItems.get(itemIndex);

                if (item.getSellerId().equals(player.getUniqueId())) {
                    player.sendMessage("§c你不能购买自己的物品！");
                    return;
                }

                double unitPrice = item.getPrice();
                int itemAmount = item.getItem().getAmount();
                double totalPrice = unitPrice * itemAmount;

                if (!PlayerMarket_Sig.getEconomy().has(player, totalPrice)) {
                    player.sendMessage(plugin.getConfigManager().getMessage("not-enough-money")
                            .replace("%price%", String.valueOf(totalPrice))
                            .replace("%balance%", String.valueOf(PlayerMarket_Sig.getEconomy().getBalance(player))));
                    return;
                }

                // 扣除买家金钱
                EconomyResponse response = PlayerMarket_Sig.getEconomy().withdrawPlayer(player, totalPrice);
                if (!response.transactionSuccess()) {
                    player.sendMessage("§c交易失败！");
                    return;
                }

                // 计算税后金额给卖家
                double taxRate = plugin.getConfigManager().getTaxRate();
                double sellerAmount = totalPrice * (1 - taxRate);

                // 给卖家金钱
                Player seller = Bukkit.getPlayer(item.getSellerId());
                if (seller != null && seller.isOnline()) {
                    PlayerMarket_Sig.getEconomy().depositPlayer(seller, sellerAmount);
                    seller.sendMessage(plugin.getConfigManager().getMessage("item-sold")
                            .replace("%item%", getItemDisplayName(item.getItem()) + " x" + itemAmount)
                            .replace("%price%", String.valueOf(sellerAmount))
                            .replace("%player%", player.getName()));
                } else {
                    // 离线玩家处理 - 这里简化处理，实际应该存储到离线玩家数据中
                    PlayerMarket_Sig.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(item.getSellerId()), sellerAmount);
                }

                // 给买家物品
                player.getInventory().addItem(item.getItem());
                player.sendMessage(plugin.getConfigManager().getMessage("item-bought")
                        .replace("%item%", getItemDisplayName(item.getItem()) + " x" + itemAmount)
                        .replace("%price%", String.valueOf(totalPrice)));

                // 从市场移除物品
                plugin.getMarketData().removeMarketItem(item.getItemId());

                // 刷新界面
                openMarketGUI(player, currentPage);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!playerInputStates.containsKey(playerId)) {
            return;
        }

        // 立即取消事件，防止消息显示在公屏
        event.setCancelled(true);
        String message = event.getMessage().trim();

        // 把所有逻辑都放到主线程执行
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (message.equalsIgnoreCase("cancel")) {
                playerInputStates.remove(playerId);
                tempItems.remove(playerId);
                tempPrices.remove(playerId);
                player.sendMessage("§c已取消上架物品。");
                return;
            }

            InputState state = playerInputStates.get(playerId);

            if (state == InputState.WAITING_PRICE) {
                // 处理价格输入
                try {
                    double price = Double.parseDouble(message);
                    if (price <= 0) {
                        player.sendMessage("§c价格必须大于0！请重新输入:");
                        return;
                    }

                    tempPrices.put(playerId, price);
                    playerInputStates.put(playerId, InputState.WAITING_AMOUNT);

                    ItemStack handItem = tempItems.get(playerId);
                    player.sendMessage("§a价格设置成功: §e" + price);
                    player.sendMessage("§a请输入要上架的数量 (1-" + handItem.getAmount() + ") (输入 'cancel' 取消):");

                } catch (NumberFormatException e) {
                    player.sendMessage("§c请输入有效的数字！");
                }

            } else if (state == InputState.WAITING_AMOUNT) {
                // 处理数量输入
                try {
                    int amount = Integer.parseInt(message);
                    ItemStack originalItem = tempItems.get(playerId);
                    double price = tempPrices.get(playerId);

                    if (amount <= 0) {
                        player.sendMessage("§c数量必须大于0！请重新输入:");
                        return;
                    }

                    if (amount > originalItem.getAmount()) {
                        player.sendMessage("§c数量不能超过手中物品数量(" + originalItem.getAmount() + ")！请重新输入:");
                        return;
                    }

                    // 检查手中物品是否还在
                    ItemStack handItem = player.getInventory().getItemInMainHand();
                    if (!handItem.isSimilar(originalItem) || handItem.getAmount() < amount) {
                        player.sendMessage("§c错误：手中物品已改变或数量不足！");
                        playerInputStates.remove(playerId);
                        tempItems.remove(playerId);
                        tempPrices.remove(playerId);
                        return;
                    }

                    // 创建要上架的物品
                    ItemStack sellItem = originalItem.clone();
                    sellItem.setAmount(amount);

                    // 从玩家背包移除指定数量的物品
                    handItem.setAmount(handItem.getAmount() - amount);
                    player.getInventory().setItemInMainHand(handItem);

                    // 添加到市场
                    plugin.getMarketData().addMarketItem(playerId, player.getName(), sellItem, price);

                    player.sendMessage("§a成功上架物品!");
                    player.sendMessage("§7物品: §e" + getItemDisplayName(sellItem) + " §7x" + amount);
                    player.sendMessage("§7单价: §e" + price + " §7总价: §e" + (price * amount));

                    // 清理临时数据
                    playerInputStates.remove(playerId);
                    tempItems.remove(playerId);
                    tempPrices.remove(playerId);

                    // 重新打开寄卖界面
                    openSellGUI(player);

                } catch (NumberFormatException e) {
                    player.sendMessage("§c请输入有效的整数！");
                }
            }
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            UUID playerId = player.getUniqueId();

            // 不要清除输入状态！玩家需要关闭界面才能输入聊天
            // 只清除页码信息
            marketPages.remove(playerId);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 玩家离线时清除所有状态，防止内存泄漏
        UUID playerId = event.getPlayer().getUniqueId();
        playerInputStates.remove(playerId);
        tempItems.remove(playerId);
        tempPrices.remove(playerId);
        marketPages.remove(playerId);
    }

    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return ChatColor.stripColor(item.getItemMeta().getDisplayName());
        }
        return item.getType().name().toLowerCase().replace("_", " ");
    }
}