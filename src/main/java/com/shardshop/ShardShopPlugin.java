package com.shardshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ShardShopPlugin extends JavaPlugin {

    public static final TextColor FUCHSIA = TextColor.color(0xFF00FF);

    private ShardManager shards;
    private ShopGUI shop;
    private NamespacedKey toolKey;

    @Override
    public void onEnable() {
        toolKey = new NamespacedKey(this, "tool");

        shards = new ShardManager(this);
        shards.load();

        shop = new ShopGUI(this, shards);
        getServer().getPluginManager().registerEvents(shop, this);
        getServer().getPluginManager().registerEvents(new ToolListener(this), this);

        // Every 1 minute (1200 ticks): give every online player 1 shard
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                shards.add(player.getUniqueId(), 1);
                player.sendMessage(
                        Component.text("You received ")
                                .append(Component.text("1 Shard", FUCHSIA))
                );
            }
            shards.save();
        }, 1200L, 1200L);
    }

    @Override
    public void onDisable() {
        if (shards != null) {
            shards.save();
        }
    }

    public NamespacedKey toolKey() {
        return toolKey;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("shardshop")) {
            shop.open(player);
        } else if (command.getName().equalsIgnoreCase("shards")) {
            player.sendMessage(
                    Component.text("You have ")
                            .append(Component.text(shards.get(player.getUniqueId()) + " Shards", FUCHSIA))
            );
        }
        return true;
    }
}
