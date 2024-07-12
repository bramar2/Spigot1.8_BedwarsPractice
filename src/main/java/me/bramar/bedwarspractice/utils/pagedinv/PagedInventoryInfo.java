package me.bramar.bedwarspractice.utils.pagedinv;

import org.bukkit.inventory.ItemStack;

class PagedInventoryInfo {
    final ItemStack[][] contents;
    final int size;
    final InventoryListener listener;
    final String rawTitle;

    PagedInventoryInfo(ItemStack[][] contents, int size, InventoryListener listener, String rawTitle) {
        this.contents = contents;
        this.size = size;
        this.listener = listener;
        this.rawTitle = rawTitle;
    }
}
