package me.bramar.bedwarspractice.blockin;

import me.bramar.bedwarspractice.BedwarsPractice;
import me.bramar.bedwarspractice.blockin.settings.BlockinSettings;
import me.bramar.bedwarspractice.utils.ModuleLogger;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public final class BlockinManager implements Listener {
    public Location lobby;
    public final List<BlockinArea> areas = new ArrayList<>();
    private final BedwarsPractice main;
    public final YamlConfiguration config;
    public final Logger logger;
    public boolean updated = false;
    private final Map<UUID, BlockinSettings> settings = new HashMap<>();
    // Items
    final ItemStack anvil, startItem, leaveItem;
    private final String iAnvil, iStart, iLeave;

    /*
    LIST OF TODO-S:

    Placeholders
    Blockin settings
    Item in hotbar functionality
    Stats
    etc

     */
    public BlockinManager() {
        main = JavaPlugin.getPlugin(BedwarsPractice.class);
        main.saveResource("blockin.yml", false);
        config = YamlConfiguration.loadConfiguration(new File(main.getDataFolder(), "blockin.yml"));
        logger = new ModuleLogger("BedwarsPractice/Blockin");
        Bukkit.getScheduler().scheduleSyncDelayedTask(main, this::load, 1L);
        // Items: just ignore
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Leave Blockin");
        iLeave = meta.getDisplayName();
        item.setItemMeta(meta);
        leaveItem = item;
        item = new ItemStack(Material.ANVIL);
        meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Blockin settings");
        iAnvil = meta.getDisplayName();
        item.setItemMeta(meta);
        anvil = item;
        item = new ItemStack(Material.WOOL, 1, (short) 0xD);
        meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Start Blockin");
        iStart = meta.getDisplayName();
        item.setItemMeta(meta);
        startItem = item;
    }
    public void load() {
        // Stats

    }
    public BedwarsPractice getPlugin() {
        return main;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        try {
            ItemStack item = e.getItem();
            ItemMeta meta = item.getItemMeta();
            String name = meta.getDisplayName();
            if(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {

            }
        }catch(Exception ignored) {}
    }
}
