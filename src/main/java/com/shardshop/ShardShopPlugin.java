package com.shardshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    /** Used by other plugins (e.g. SimpleShop) to charge shards. */
    public ShardManager shards() {
        return shards;
    }

    public NamespacedKey toolKey() {
        return toolKey;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();

        if (name.equals("giveshards")) {
            return handleGiveShards(sender, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (name.equals("shardshop")) {
            shop.open(player);
        } else if (name.equals("shards")) {
            player.sendMessage(
                    Component.text("You have ")
                            .append(Component.text(shards.get(player.getUniqueId()) + " Shards", FUCHSIA))
            );
        }
        return true;
    }

    private boolean handleGiveShards(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /giveshards <player> <amount>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("That player is not online.", NamedTextColor.RED));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Amount must be a number.", NamedTextColor.RED));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(Component.text("Amount must be above 0.", NamedTextColor.RED));
            return true;
        }

        shards.add(target.getUniqueId(), amount);
        shards.save();

        sender.sendMessage(
                Component.text("Gave ")
                        .append(Component.text(amount + " Shards", FUCHSIA))
                        .append(Component.text(" to " + target.getName() + "."))
        );
        target.sendMessage(
                Component.text("You received ")
                        .append(Component.text(amount + " Shards", FUCHSIA))
        );
        return true;
    }
}
