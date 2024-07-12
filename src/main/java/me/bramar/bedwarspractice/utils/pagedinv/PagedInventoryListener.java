package me.bramar.bedwarspractice.utils.pagedinv;

import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PagedInventoryListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        try {
            InventoryListener listener = PagedInventory.listeners.get(e.getView().getTitle());
            PagedInventoryInfo info = PagedInventory.invInfo.get(e.getView().getTitle());
            if(listener != null && info != null) {
                if(e.getClickedInventory() instanceof PlayerInventory) {
                    if(e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) e.setCancelled(true);
                    return;
                }else e.setCancelled(true);
                Inventory inv = e.getView().getTopInventory();
                PagedAction action = e.getSlot() == inv.getSize() - 9 ? PagedAction.PREVIOUS :
                        (e.getSlot() == inv.getSize() - 1 ? PagedAction.NEXT : PagedAction.NONE);
                ItemStack pageItem = inv.getItem(inv.getSize() - 5);
                NBTTagCompound pageTag = PagedInventory.getNBTTag(pageItem);
                int currentPage = pageTag.getInt(PagedInventory.CURRENT_PAGE);
                int maxPage = pageTag.getInt(PagedInventory.MAX_PAGE);
                if(action == PagedAction.NONE) listener.run(e, currentPage, maxPage, action);
                else {
                    int newPage = currentPage + action.value;
                    if(newPage > maxPage) newPage = 1;
                    if(newPage < 1) newPage = maxPage;
                    PagedInventory.listeners.remove(e.getView().getTitle());
                    e.getWhoClicked().openInventory(
                            PagedInventory.create(
                                    info.rawTitle,
                                    info.size,
                                    maxPage,
                                    newPage,
                                    info.contents,
                                    listener
                            )
                    );
                }
            }
        }catch(Exception ignored) {}
    }
}
