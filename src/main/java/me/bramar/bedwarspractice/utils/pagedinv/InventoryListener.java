package me.bramar.bedwarspractice.utils.pagedinv;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface InventoryListener {
    void run(@NotNull InventoryClickEvent e, int currentPage, int maxPage, @NotNull PagedAction pagedAction);
}
