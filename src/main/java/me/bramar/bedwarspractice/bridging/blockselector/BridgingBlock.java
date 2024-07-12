package me.bramar.bedwarspractice.bridging.blockselector;

import me.bramar.bedwarspractice.bridging.BridgingManager;
import me.bramar.bedwarspractice.bridging.BridgingStats;
import me.bramar.bedwarspractice.utils.pagedinv.PagedInventory;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BridgingBlock {
    private final ItemStack item;
    private final int coins;
    private final boolean disabled, isDefault;
    private final String id;
    private final String permission;
    private final BridgingManager manager;
    public BridgingBlock(BridgingManager manager, ConfigurationSection section) {
        this.manager = manager;
        item = new ItemStack(Material.valueOf(section.getString("mat")), 64, convertShort(section.getInt("data", 0)));
        id = section.getString("id");
        if(id == null) throw new NullPointerException();
        coins = section.getInt("coins", 0);
        disabled = section.getBoolean("disabled", false);
        isDefault = section.getBoolean("default", false);
        String title = section.getString("name", null);
        List<String> lore = section.getStringList("lore");
        permission = section.getString("permission");
        ItemMeta meta = item.getItemMeta();
        boolean a = title != null;
        boolean b = !lore.isEmpty();
        boolean c = meta != null;
        if(a && c) {
            title = ChatColor.translateAlternateColorCodes('&', title);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', title));
        }
        if(b && c) {
            lore = lore.stream().map((s) -> ChatColor.translateAlternateColorCodes('&', s)).collect(Collectors.toList());
            meta.setLore(lore);
        }
        if(a || b) item.setItemMeta(meta);
    }
    private short convertShort(int i) {
        return (i > Short.MAX_VALUE) ? Short.MAX_VALUE : (i < Short.MIN_VALUE) ? Short.MIN_VALUE : (short) i;
    }

    public String getId() {
        return id;
    }

    public int getPrice() {
        return coins;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public ItemStack getItem(Player p) {
        ItemStack item = this.item.clone();
        item.setAmount(item.getMaxStackSize());
        if(manager.hasPlaceholderAPI()) {
            ItemMeta meta = item.getItemMeta();
            String title = meta.getDisplayName();
            if(title != null) meta.setDisplayName(PlaceholderAPI.setPlaceholders(p, title));
            List<String> lore = meta.getLore();
            if(lore != null) {
                lore = new ArrayList<>(lore).stream()
                .map((str) -> PlaceholderAPI.setPlaceholders(p, str))
                .collect(Collectors.toList());
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getShopItem(Player p) {
        ItemStack item = this.item.clone();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        if(lore == null) lore = new ArrayList<>();
        else lore = new ArrayList<>(lore);
        BridgingStats stats = manager.getStats(p.getUniqueId());
        lore.addAll(Arrays.asList("",
                ChatColor.YELLOW + "Price: " + (isDefault ? "None!" : coins),
                ChatColor.DARK_AQUA + "Owned: " + (stats.ownBlock(id) ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No")));
        if(permission != null) lore.add(ChatColor.GRAY + "Permission: " + (p.hasPermission(permission) ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        if(disabled) lore.add(ChatColor.RED + "This block is currently disabled");
        lore.add("");
        if(!stats.ownBlock(id)) {
            if(stats.getCoins() >= coins && !isDefault) lore.add(ChatColor.GREEN + "You have enough coins to buy this.");
            else if(isDefault) lore.add(ChatColor.GOLD + "You have this block by default");
            else lore.add(ChatColor.RED + "You do not have enough coins to buy this.");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return PagedInventory.setNBTTags(item, (tag) -> {
            tag.setString("block_id", id);
            return tag;
        });
    }
}
