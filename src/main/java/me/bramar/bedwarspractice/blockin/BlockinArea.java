package me.bramar.bedwarspractice.blockin;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.bramar.bedwarspractice.BedwarsPractice;
import me.bramar.bedwarspractice.utils.WorldEditAPI;
import org.bukkit.Location;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class BlockinArea implements Listener {
    private final BlockinManager manager;
    private final ConfigurationSection config;
    private String name;
    private World world;
    private Location spawn;
    private ProtectedRegion blockGuard, spawnGuard, dropGuard;
    private CuboidClipboard schematic;
    private Location pasteLocation;
    private boolean closed = false;
    private boolean available = true;
    // Currently
    private Player p;
    // WorldEdit
    private WorldEdit we;
    private WorldEditPlugin wep;
    // Timer
    private boolean timer = false;
    private int timerTicks = 0;


    BlockinArea(BlockinManager manager, ConfigurationSection section) {
        this.manager = manager;
        this.config = section;
        Bukkit.getScheduler().scheduleSyncDelayedTask(manager.getPlugin(), this::load, 1L);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(manager.getPlugin(), () -> {
            if(timer) timerTicks++;
        }, 1, 1);
    }
    public void load() {
        we = WorldEdit.getInstance();
        wep = JavaPlugin.getPlugin(WorldEditPlugin.class);
        name = config.getString("name", "(name not found)");
        try {
            String worldName = config.getString("world");
            if(worldName == null) throw new NullPointerException("World is not specified in config");
            this.world = Bukkit.getWorld(worldName);
            if(this.world == null) {
                // try to load
                JavaPlugin.getPlugin(BedwarsPractice.class).getLogger()
                        .log(Level.INFO, "Attempting to load world " + worldName);
                this.world = Bukkit.getServer().createWorld(new WorldCreator(worldName));
            }
            if(this.world == null) throw new NullPointerException("World does not exist (null).");
            // Spawn
            ConfigurationSection spawnConfig = config.getConfigurationSection("spawn");
            if(spawnConfig == null) throw new NullPointerException("Spawn is not specified in config");
            worldName = spawnConfig.getString("world");
            if(worldName == null) throw new NullPointerException("World of spawn location is not specified in config");
            World world = Bukkit.getWorld(worldName);
            if(world == null) throw new NullPointerException("World of spawn location does not exist/has not loaded yet.");
            if(this.world != world) throw new IllegalStateException("Area world and spawn location world must be the same");
            spawn = new Location(world, spawnConfig.getDouble("x"), spawnConfig.getDouble("y"), spawnConfig.getDouble("z"));
            if(spawnConfig.contains("yaw")) spawn.setYaw((float) spawnConfig.getDouble("yaw"));
            if(spawnConfig.contains("pitch")) spawn.setPitch((float) spawnConfig.getDouble("pitch"));
            // Schematic & Paste location
            schematic = WorldEditAPI.getSchematic(config.getString("schematic_file"));

            ConfigurationSection pasteLoc = config.getConfigurationSection("paste_location");
            if(pasteLoc == null) throw new NullPointerException("Spawn is not specified in config");
            worldName = pasteLoc.getString("world");
            if(worldName == null) throw new NullPointerException("World of paste location is not specified in config");
            world = Bukkit.getWorld(worldName);
            if(world == null) throw new NullPointerException("World of paste location does not exist/has not loaded yet.");
            if(this.world != world) throw new IllegalStateException("Area world and paste location world must be the same");
            pasteLocation = new Location(world, pasteLoc.getDouble("x"), pasteLoc.getDouble("y"), pasteLoc.getDouble("z"));
            if(pasteLoc.contains("yaw")) pasteLocation.setYaw((float) pasteLoc.getDouble("yaw"));
            if(pasteLoc.contains("pitch")) pasteLocation.setPitch((float) pasteLoc.getDouble("pitch"));

            /*
            Regions: blockGuard (region of placing blocks),
                     dropGuard (region where blocks will be broken to dropdown),
                     spawnGuard (region of setting up)
             */
            RegionManager rm = WorldGuardPlugin.inst().getRegionManager(world);
            String blockName = config.getString("block_worldguard");
            String dropName = config.getString("drop_worldguard");
            String spawnName = config.getString("spawn_worldguard");
            if(blockName == null || dropName == null || spawnName == null)
                throw new NullPointerException("Block/drop/spawn worldguard is not specified in config");
            blockGuard = rm.getRegion(blockName);
            dropGuard = rm.getRegion(dropName);
            spawnGuard = rm.getRegion(spawnName);
            if(blockGuard == null || dropGuard == null || spawnGuard == null)
                throw new NullPointerException("Block/drop/spawn worldguard does not exist in world " + world.getName());


            closed = config.getBoolean("closed", false);
        }catch(Exception e1) {
            manager.logger.log(Level.WARNING, "BlockinArea of " + name + " could not be loaded!", e1);
            closed = true;
        }
    }
    public void join(Player p) {
        if(available && !closed) {
            available = false;
            this.p = p;
            resetBlocks();
            p.teleport(spawn);
            p.getInventory().clear();
            p.getInventory().setArmorContents(new ItemStack[4]);
            setInventory(p);
            p.sendMessage(ChatColor.GREEN + "You joined area " + ChatColor.DARK_GREEN + name);
            try {
                LocalWorld world = BukkitUtil.getLocalWorld(p.getWorld());
                int y1 = dropGuard.getMinimumPoint().getBlockY();
                int y2 = dropGuard.getMaximumPoint().getBlockY();
                int minY = Math.min(y1, y2);
                int maxY = Math.max(y1, y2);
                WorldEditAPI.updateChunks(new Polygonal2DRegion(world, dropGuard.getPoints(), minY, maxY), p);
            }catch(Exception ignored) {}
        }else throw new IllegalStateException("Area not available!");
    }
    public void leave() {
        if(available || p == null) return;
        p.getInventory().clear();
        p.sendMessage(ChatColor.GREEN + "You left the area");
        p = null;
        available = true;
    }

    public Player getPlayer() {
        return p;
    }
    public void setInventory(Player p) {
        p.getInventory().setItem(4, manager.startItem.clone());
        p.getInventory().setItem(7, manager.anvil.clone());
        p.getInventory().setItem(8, manager.leaveItem.clone());
    }
    public void resetBlocks() {
        WorldEditAPI.paste(schematic, pasteLocation);
    }

    private void start() {

        WorldEdit we = WorldEdit.getInstance();
        LocalWorld world = BukkitUtil.getLocalWorld(p.getWorld());
        EditSession session = we.getEditSessionFactory().getEditSession(world, -1);
        session.enableQueue();
        BaseBlock air = new BaseBlock(BlockID.AIR);
        int y1 = dropGuard.getMinimumPoint().getBlockY();
        int y2 = dropGuard.getMaximumPoint().getBlockY();
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        Polygonal2DRegion polygonal = new Polygonal2DRegion(world, dropGuard.getPoints(), minY, maxY);
        try {
            session.setBlocks(polygonal, air);
        }catch(MaxChangedBlocksException ignored) {} // Impossible since maxBlocks is -1 (no limit)
        session.commit();
        Bukkit.getScheduler().scheduleSyncDelayedTask(manager.getPlugin(), () -> {
            // Undo 3 seconds later
            session.undo(we.getEditSessionFactory().getEditSession(world, -1));
            session.flushQueue();
            Bukkit.getScheduler().scheduleSyncDelayedTask(manager.getPlugin(), () -> {
                WorldEditAPI.updateChunks(polygonal, p);
            }, 1);
        }, 3 * 20);

    }

    protected void onRightClickStart() {
        Player player = p;
        manager.getPlugin().startXPCooldown(player, 30, "&aGO!", () -> {
            // If didn't rejoin another area/leave area
            if(p != null && player.isOnline() && player.getUniqueId().equals(p.getUniqueId()))
                start();
        });
    }

    public void stop() {
        timer = false;
        resetBlocks();
        p.teleport(spawn);
        double seconds = timerTicks / 20d;
        p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&aYou finished in &6" + seconds + "&as on &c" + p.getHealth() + "&a HP!"));
        p.sendTitle(ChatColor.GREEN.toString() + seconds + "s", null);
        p.playSound(p.getLocation(), Sound.LEVEL_UP, 1, 1);
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        if(closed) return;
        config.set("closed", true);
        manager.updated = true;
        closed = true;
        HandlerList.unregisterAll(this);
    }
    public void open() {
        if(!closed) return;
        config.set("closed", false);
        manager.updated = true;
        closed = false;
        Bukkit.getServer().getPluginManager().registerEvents(this, manager.getPlugin());
    }

    public boolean isAvailable() {
        return available;
    }

    private Location toLocation(World world, Vector vector) {
        return new Location(world, vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        if(p != null && e.getPlayer().getUniqueId().equals(p.getUniqueId())) stop();
    }
    @EventHandler
    public void onFallDamage(EntityDamageEvent e) {
        if(p != null && p.getUniqueId().equals(e.getEntity().getUniqueId()))
            e.setCancelled(true);
    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if(p != null && p.getUniqueId().equals(e.getPlayer().getUniqueId())) {
            Location loc = e.getBlock().getLocation();
            if(!blockGuard.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) e.setCancelled(true);
        }
    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if(p != null && p.getUniqueId().equals(e.getPlayer().getUniqueId())) {
            Location loc = e.getBlock().getLocation();
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            if(blockGuard.contains(x, y, z) && e.getBlock().getType() == Material.BED_BLOCK) stop();
            else e.setCancelled(true);
        }
    }
}
