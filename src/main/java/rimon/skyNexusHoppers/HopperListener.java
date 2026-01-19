package rimon.skyNexusHoppers;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.block.TileState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class HopperListener implements Listener {

    private final SkyNexusHoppers plugin;

    public HopperListener(SkyNexusHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack handItem = event.getItemInHand();

        if (handItem.getItemMeta() != null &&
                handItem.getItemMeta().getPersistentDataContainer().has(SkyNexusHoppers.CHUNK_HOPPER_KEY, PersistentDataType.BYTE)) {

            BlockState state = event.getBlockPlaced().getState();
            if (state instanceof TileState) {
                TileState tileState = (TileState) state;
                tileState.getPersistentDataContainer().set(SkyNexusHoppers.CHUNK_HOPPER_KEY, PersistentDataType.BYTE, (byte) 1);
                tileState.update();

                String locationStr = String.format("World: %s, X: %d, Y: %d, Z: %d",
                        event.getBlockPlaced().getWorld().getName(),
                        event.getBlockPlaced().getX(),
                        event.getBlockPlaced().getY(),
                        event.getBlockPlaced().getZ());

                plugin.getLogger().info("[Placement] Player " + event.getPlayer().getName() +
                        " placed a Chunk Hopper at " + locationStr);
                event.getPlayer().sendMessage("" + ChatColor.GREEN + "Chunk Hopper placed!");
            }
        }
    }

    // --- 2. Handle Breaking ---
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        BlockState state = event.getBlock().getState();
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (state instanceof TileState) {
            TileState tileState = (TileState) state;

            if (tileState.getPersistentDataContainer().has(SkyNexusHoppers.CHUNK_HOPPER_KEY, PersistentDataType.BYTE)) {
                ItemStack itemToGive = plugin.getChunkHopperItem(1);

                if (player.getInventory().firstEmpty() == -1) {
                    event.setCancelled(true);
                    String msg = plugin.getConfig().getString("messages.inventory-full", "&cInventory Full!");
                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
                    return;
                }

                event.setDropItems(false);
                player.getInventory().addItem(itemToGive);
                String locationStr = String.format("World: %s, X: %d, Y: %d, Z: %d",
                        event.getBlock().getWorld().getName(),
                        event.getBlock().getX(),
                        event.getBlock().getY(),
                        event.getBlock().getZ());

                plugin.getLogger().info("[Break] Player " + player.getName() +
                        " broke a Chunk Hopper at " + locationStr);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item itemEntity = event.getEntity();
        ItemStack stackToInsert = itemEntity.getItemStack().clone();

        boolean filterEnabled = plugin.getConfig().getBoolean("filter.enabled", false);
        if (filterEnabled) {
            List<String> whitelist = plugin.getConfig().getStringList("filter.whitelist");
            String itemType = stackToInsert.getType().toString();

            if (!whitelist.contains(itemType)) {
                return;
            }
        }

        Chunk chunk = event.getLocation().getChunk();
        List<Hopper> chunkHoppers = new ArrayList<>();

        for (BlockState tile : chunk.getTileEntities()) {
            if (tile instanceof Hopper) {
                Hopper hopper = (Hopper) tile;
                if (hopper.getPersistentDataContainer().has(SkyNexusHoppers.CHUNK_HOPPER_KEY, PersistentDataType.BYTE)) {
                    chunkHoppers.add(hopper);
                }
            }
        }

        if (chunkHoppers.isEmpty()) return;

        Collections.shuffle(chunkHoppers);

        boolean fullyPickedUp = false;

        for (Hopper hopper : chunkHoppers) {
            HashMap<Integer, ItemStack> leftovers = hopper.getInventory().addItem(stackToInsert);

            if (leftovers.isEmpty()) {
                fullyPickedUp = true;
                break;
            }
            stackToInsert = leftovers.get(0);
        }

        if (fullyPickedUp) {
            event.setCancelled(true);
        } else {
            itemEntity.setItemStack(stackToInsert);
        }
    }
}
