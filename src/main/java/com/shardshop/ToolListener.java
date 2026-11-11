package com.shardshop;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ToolListener implements Listener {

    private static final int MAX_LOGS = 300;
    private static final int MAX_RADIUS = 12;

    private final ShardShopPlugin plugin;
    /** Players currently mid-break, so our own breakBlock calls don't trigger this again. */
    private final Set<UUID> working = new HashSet<>();

    public ToolListener(ShardShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (working.contains(player.getUniqueId())) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType() == Material.AIR || !tool.hasItemMeta()) return;

        String type = tool.getItemMeta().getPersistentDataContainer()
                .get(plugin.toolKey(), PersistentDataType.STRING);
        if (type == null) return;

        if (type.equals("pickaxe") && tool.getType() == Material.NETHERITE_PICKAXE) {
            breakArea(player, event.getBlock(), tool);
        } else if (type.equals("axe") && tool.getType() == Material.NETHERITE_AXE) {
            if (Tag.LOGS.isTagged(event.getBlock().getType())) {
                chopTree(player, event.getBlock());
            }
        }
    }

    // ---------- 3x3 pickaxe ----------

    private void breakArea(Player player, Block origin, ItemStack tool) {
        BlockFace face = faceOf(player, origin);

        List<Block> targets = new ArrayList<>();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                Block target = switch (face) {
                    case UP, DOWN -> origin.getRelative(a, 0, b);
                    case NORTH, SOUTH -> origin.getRelative(a, b, 0);
                    default -> origin.getRelative(0, b, a);
                };
                targets.add(target);
            }
        }

        working.add(player.getUniqueId());
        try {
            for (Block target : targets) {
                if (player.getInventory().getItemInMainHand().getType() != Material.NETHERITE_PICKAXE) break;
                if (target.getType().isAir() || target.isLiquid()) continue;
                if (target.getType().getHardness() < 0) continue;
                if (!target.isPreferredTool(tool)) continue;
                player.breakBlock(target);
            }
        } finally {
            working.remove(player.getUniqueId());
        }
    }

    private BlockFace faceOf(Player player, Block origin) {
        RayTraceResult result = player.rayTraceBlocks(8.0);
        if (result != null && origin.equals(result.getHitBlock()) && result.getHitBlockFace() != null) {
            return result.getHitBlockFace();
        }
        float pitch = player.getLocation().getPitch();
        if (pitch > 45) return BlockFace.UP;
        if (pitch < -45) return BlockFace.DOWN;
        return player.getFacing().getOppositeFace();
    }

    // ---------- tree chopping axe ----------

    private void chopTree(Player player, Block origin) {
        Set<Block> logs = new LinkedHashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        logs.add(origin);
        queue.add(origin);
        boolean naturalTree = false;

        while (!queue.isEmpty() && logs.size() < MAX_LOGS) {
            Block current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block next = current.getRelative(dx, dy, dz);

                        if (Math.abs(next.getX() - origin.getX()) > MAX_RADIUS
                                || Math.abs(next.getZ() - origin.getZ()) > MAX_RADIUS) continue;

                        if (Tag.LOGS.isTagged(next.getType())) {
                            if (logs.add(next)) queue.add(next);
                        } else if (!naturalTree
                                && next.getBlockData() instanceof Leaves leaves
                                && !leaves.isPersistent()) {
                            naturalTree = true;
                        }
                    }
                }
            }
        }

        // Only chop real trees (logs touching natural leaves), not player-built log structures
        if (!naturalTree) return;

        logs.remove(origin); // the original block is broken by the event itself

        working.add(player.getUniqueId());
        try {
            for (Block log : logs) {
                if (player.getInventory().getItemInMainHand().getType() != Material.NETHERITE_AXE) break;
                if (!Tag.LOGS.isTagged(log.getType())) continue;
                player.breakBlock(log);
            }
        } finally {
            working.remove(player.getUniqueId());
        }
    }
}
