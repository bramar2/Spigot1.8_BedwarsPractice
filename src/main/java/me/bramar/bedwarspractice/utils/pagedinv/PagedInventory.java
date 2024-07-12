package me.bramar.bedwarspractice.utils.pagedinv;

import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class PagedInventory {
    static final Map<String, InventoryListener> listeners = new HashMap<>();
    static final Map<String, PagedInventoryInfo> invInfo = new HashMap<>();
    static final String CURRENT_PAGE = "currentPage";
    static final String MAX_PAGE = "maxPage";
    private PagedInventory() {}
    public static ItemStack setNBTTags(ItemStack item, Function<NBTTagCompound, @NotNull NBTTagCompound> function) {
        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
        nmsStack.setTag(nmsStack.hasTag() ? function.apply(nmsStack.getTag()) : function.apply(new NBTTagCompound()));
        return CraftItemStack.asBukkitCopy(nmsStack);
    }
    public static NBTTagCompound getNBTTag(ItemStack item) {
        NBTTagCompound n = CraftItemStack.asNMSCopy(item).getTag();
        return (n == null) ? new NBTTagCompound() : n;
    }
    private static ItemStack newItem(Material mat, String title, short data) {
        ItemStack item = new ItemStack(mat, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', title));
        item.setItemMeta(meta);
        return item;
    }
    public static Inventory create(String title, int size, int pages, int currentPage, ItemStack[][] contents, InventoryListener listener) {
        String cTitle = ChatColor.translateAlternateColorCodes('&', title) + " [" + UUID.randomUUID().toString().substring(0, 4) + "]";
        if(contents.length < pages) throw new IllegalArgumentException("Contents[][] length must be same/higher as pages. Array length: " + contents.length + ", Pages: " + pages);
        listeners.remove(cTitle);
        invInfo.remove(cTitle);
        Inventory inv = Bukkit.createInventory(null, size, cTitle);
        for(int i = 0; i < Math.min(contents[currentPage-1].length, size - 18); i++) {
            inv.setItem(i, contents[currentPage-1][i]);
        }
        ItemStack glass = newItem(Material.STAINED_GLASS_PANE, " ", (short) 15);
        for(int i = size - 18; i < size - 9; i++) {
            inv.setItem(i, glass);
        }
        ItemStack arrow1 = newItem(Material.ARROW, "&fPrevious", (short) 0);
        ItemStack arrow2 = newItem(Material.ARROW, "&fNext", (short) 0);
        ItemStack page = newItem(Material.PAPER, "&fCurrent Page: " + currentPage, (short) 0);
        ItemMeta meta = page.getItemMeta();
        meta.setLore(Collections.singletonList(ChatColor.WHITE + "Max Page: " + pages));
//        meta.getPersistentDataContainer().set(CURRENT_PAGE, PersistentDataType.INTEGER, currentPage);
//        meta.getPersistentDataContainer().set(MAX_PAGE, PersistentDataType.INTEGER, pages);
        page.setItemMeta(meta);
        page = setNBTTags(page, (tag) -> {
           tag.setInt(CURRENT_PAGE, currentPage);
           tag.setInt(MAX_PAGE, pages);
           return tag;
        });
        inv.setItem(size - 9, arrow1);
        inv.setItem(size - 1, arrow2);
        inv.setItem(size - 5, page);
        listeners.put(cTitle, listener);
        invInfo.put(cTitle, new PagedInventoryInfo(contents, size, listener, title));
        return inv;
    }
}
