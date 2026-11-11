package com.shardshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ShopGUI implements Listener {

    public static final int COST = 100;
    private static final int PICKAXE_SLOT = 11;
    private static final int AXE_SLOT = 15;

    private final ShardShopPlugin plugin;
    private final ShardManager shards;

    public ShopGUI(ShardShopPlugin plugin, ShardManager shards) {
        this.plugin = plugin;
        this.shards = shards;
    }

    /** Marks an inventory as the shop so we can recognise it in click events. */
    private static class ShopHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public void open(Player player) {
        ShopHolder holder = new ShopHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Shard Shop"));
        holder.inventory = inv;

        inv.setItem(PICKAXE_SLOT, display(createTool("pickaxe")));
        inv.setItem(AXE_SLOT, display(createTool("axe")));

        player.openInventory(inv);
    }

    private static Component plain(String text, TextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    /** The real item the player receives. */
    private ItemStack createTool(String type) {
        boolean pickaxe = type.equals("pickaxe");
        ItemStack item = new ItemStack(pickaxe ? Material.NETHERITE_PICKAXE : Material.NETHERITE_AXE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(plain(pickaxe ? "Shard Pickaxe" : "Shard Axe", ShardShopPlugin.FUCHSIA));
        meta.lore(List.of(
                plain(pickaxe ? "Breaks a 3x3 area" : "Chops down a whole tree", NamedTextColor.GRAY)
        ));
        meta.getPersistentDataContainer().set(plugin.toolKey(), PersistentDataType.STRING, type);

        item.setItemMeta(meta);
        return item;
    }

    /** The version shown in the shop (adds price lines). */
    private ItemStack display(ItemStack tool) {
        ItemStack item = tool.clone();
        ItemMeta meta = item.getItemMeta();

        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.add(Component.empty());
        lore.add(plain("Cost: ", NamedTextColor.GRAY)
                .append(plain(COST + " Shards", ShardShopPlugin.FUCHSIA)));
        lore.add(plain("Click to buy", NamedTextColor.YELLOW));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof ShopHolder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != top) return;

        if (event.getSlot() == PICKAXE_SLOT) {
            buy(player, "pickaxe", "Shard Pickaxe");
        } else if (event.getSlot() == AXE_SLOT) {
            buy(player, "axe", "Shard Axe");
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ShopHolder) {
            event.setCancelled(true);
        }
    }

    private void buy(Player player, String type, String name) {
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(Component.text("Your inventory is full.", NamedTextColor.RED));
            return;
        }

        if (!shards.take(player.getUniqueId(), COST)) {
            player.sendMessage(
                    Component.text("You need ", NamedTextColor.RED)
                            .append(Component.text(COST + " Shards", ShardShopPlugin.FUCHSIA))
                            .append(Component.text(" but you only have ", NamedTextColor.RED))
                            .append(Component.text(shards.get(player.getUniqueId()), ShardShopPlugin.FUCHSIA))
                            .append(Component.text(".", NamedTextColor.RED))
            );
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        player.getInventory().addItem(createTool(type));
        player.sendMessage(
                Component.text("You bought ")
                        .append(Component.text(name, ShardShopPlugin.FUCHSIA))
                        .append(Component.text("!"))
        );
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }
}
