package rimon.skyNexusHoppers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SkyNexusHoppers extends JavaPlugin implements CommandExecutor {

    public static SkyNexusHoppers instance;
    public static NamespacedKey CHUNK_HOPPER_KEY;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        CHUNK_HOPPER_KEY = new NamespacedKey(this, "is_chunk_hopper");

        getCommand("snh").setExecutor(this);
        getServer().getPluginManager().registerEvents(new HopperListener(this), this);

        getLogger().info("SkyNexusHoppers has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SkyNexusHoppers has been disabled!");
    }

    public ItemStack getChunkHopperItem(int amount) {
        ItemStack item = new ItemStack(Material.HOPPER, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = getConfig().getString("item.name", "&b&lChunk Hopper");
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));

            List<String> loreConfig = getConfig().getStringList("item.lore");
            List<Component> lore = new ArrayList<>();
            for (String line : loreConfig) {
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
            }
            meta.lore(lore);

            meta.getPersistentDataContainer().set(CHUNK_HOPPER_KEY, PersistentDataType.BYTE, (byte) 1);

            if (getConfig().getBoolean("item.glint")) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("skynexushoppers.admin")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getConfig().getString("messages.no-permission")));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                sender.sendMessage(Component.text("Usage: /snh give <player> <amount>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getConfig().getString("messages.player-not-found")));
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getConfig().getString("messages.invalid-amount")));
                return true;
            }

            target.getInventory().addItem(getChunkHopperItem(amount));

            String prefix = getConfig().getString("messages.prefix");
            String giveMsg = getConfig().getString("messages.give-success")
                    .replace("%player%", target.getName())
                    .replace("%amount%", String.valueOf(amount));
            String receiveMsg = getConfig().getString("messages.receive")
                    .replace("%amount%", String.valueOf(amount));

            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + giveMsg));
            target.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + receiveMsg));
            return true;
        }

        return false;
    }
}
