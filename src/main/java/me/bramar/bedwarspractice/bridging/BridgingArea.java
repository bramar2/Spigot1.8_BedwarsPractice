package me.bramar.bedwarspractice.bridging;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.bramar.bedwarspractice.worldguard.PlayerEnterRegionEvent;
import me.bramar.bedwarspractice.worldguard.PlayerExitRegionEvent;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class BridgingArea implements Listener {
    private boolean available = true;

    private String areaName;
    private World world;
    private Location spawnpoint;
    private ProtectedRegion finishLine;
    private ProtectedRegion areaGuard;
    private final BridgingManager manager;
    private final ConfigurationSection config;
    private int timerTicks = 0;
    private boolean closed = false;
    private String areaGroup = "default";
    private boolean timer = false; // enabled
    private final List<Block> placedBlocks = new ArrayList<>();
    private final List<Block> lastPlacedBlocks = new ArrayList<>();
    public final int repeatingTaskId;

    BridgingArea(BridgingManager manager, ConfigurationSection config) {
        Bukkit.getPluginManager().registerEvents(this, manager.getPlugin());
        this.config = config;
        this.manager = manager;
        Bukkit.getScheduler().scheduleSyncDelayedTask(manager.getPlugin(), this::load, 1);
        repeatingTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(manager.getPlugin(), () -> {
            if(timer) {
                timerTicks++;
                // This will run every 2 ticks
                if(timerTicks % 2 == 0) sendActionbar(p, ChatColor.GOLD + "Time: " + ChatColor.GREEN + ((double) timerTicks / 20d) + "s" + ChatColor.GOLD + " | Blocks: " + ChatColor.GREEN + placedBlocks.size());

                if(timerTicks >= 2400) stopTimerFail();
            }
        }, 1, 1);
    }
    private void sendActionbar(Player p, String str) {
        // 1.8
        PacketPlayOutChat packet = new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a("\"" + str + "\""), (byte) 2);
        if(p instanceof CraftPlayer) ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
    }
    private void load() {
        try {
            areaName = config.getString("name").replace(" ", "_");
            areaGroup = manager.getAreaGroup(areaName);
            String worldName = config.getString("world", "");
            world = Bukkit.getWorld(worldName);

            if(world == null) {
                manager.getPlugin().getLogger()
                        .log(Level.INFO, "Attempting to load world " + worldName);
                world = Bukkit.getServer().createWorld(new WorldCreator(worldName));
            }

            ConfigurationSection spawnpoint = config.getConfigurationSection("spawnpoint");
            this.spawnpoint = new Location(world, spawnpoint.getDouble("x"), spawnpoint.getDouble("y"), spawnpoint.getDouble("z"));
            if(config.contains("yaw")) this.spawnpoint.setYaw((float) config.getDouble("yaw"));
            if(config.contains("pitch")) this.spawnpoint.setPitch((float) config.getDouble("pitch"));
            RegionManager regions = WorldGuardPlugin.inst().getRegionManager(world);
            finishLine = regions.getRegion(config.getString("finish_line"));
            areaGuard = regions.getRegion(config.getString("worldguard"));
            closed = config.getBoolean("closed", false);
            if(config.getString("world") == null) throw new NullPointerException("Missing world info!");
            if(world == null) throw new NullPointerException("World does not exist. (null)");
            if(areaName == null || finishLine == null || areaGuard == null) throw new NullPointerException("Missing one of these: Area name, finish line, area worldguard");
            if(!areaGuard.contains(this.spawnpoint.getBlockX(), this.spawnpoint.getBlockY(), this.spawnpoint.getBlockZ())) throw new IllegalArgumentException("The spawnpoint is not in the area! Adjust it using /bridging setspawn");
            manager.logger.info("Loaded " + areaName + " BridgingArea (with group '" + areaGroup + "')!");
        }catch(Exception e1) {
            manager.logger.log(Level.WARNING, "BridgingArea of " + config.getString("name", "(unnamed)") + " could not be loaded!",
                    e1);
            closed = true;
        }
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

    public World getWorld() {
        return world;
    }

    public void open() {
        if(!closed) return;
        config.set("closed", false);
        manager.updated = true;
        closed = false;
        Bukkit.getPluginManager().registerEvents(this, manager.getPlugin());
    }

    public boolean isTimerDisabled() {
        return !timer;
    }

    public double getSecondsTimer() {
        return (double) timerTicks / 20d;
    }

    public String getAreaGroup() {
        return areaGroup;
    }

    public void setAreaGroup(String areaGroup) {
        this.areaGroup = areaGroup;
    }

    public void startTimer() {
        timer = true;
        timerTicks = 0;
    }
    public void stopTimer() {
        timer = false;
        p.teleport(spawnpoint);
        clearBlocks();
        double secondsTime = (double) timerTicks / 20d;
        String seconds = secondsTime + "s";
        p.sendMessage(ChatColor.GREEN + "You finished in " + seconds);
        int coins = giveCoinsAmount(true, false);

        p.sendTitle(ChatColor.GREEN + seconds, ChatColor.GOLD + "+" + coins + " coins");
        p.playSound(p.getLocation(), Sound.LEVEL_UP, 1, 1);
        if(stats == null) stats = manager.getStats(p.getUniqueId());
        stats.addCoins(coins);
        stats.addPlacedBlocks(lastPlacedBlocks.size());
        stats.setBridgeCount(stats.getBridgeCount() + 1);
        if((stats.getPersonalBest() > secondsTime || stats.getPersonalBest() <= 0.0d) && secondsTime >= 0.0001d) {
            p.sendMessage(ChatColor.GOLD + "===== NEW GLOBAL PERSONAL BEST =====\n" +
                    "Previous Global PB: " + ChatColor.GREEN + stats.getPersonalBest() + "s\n" +
                    "New Global PB: " + ChatColor.GREEN + seconds + "\n" +
                    ChatColor.YELLOW + "Global means: per every group and not only 16, 20, 32 blocks\n" +
                    ChatColor.GOLD + "===== NEW GLOBAL PERSONAL BEST =====");
            stats.setPersonalBest(secondsTime);
        }

        double specificPB = stats.getSpecificPB(areaGroup);

        if((specificPB > secondsTime || specificPB <= 0.0d) && secondsTime >= 0.0001d) {
            p.sendMessage(ChatColor.GOLD + "===== NEW GROUP PERSONAL BEST =====\n" +
                    "At group: " + ChatColor.GREEN + areaGroup + "\n" +
                    "Previous Group PB: " + ChatColor.GREEN + specificPB + "s\n" +
                    "New Group PB: " + ChatColor.GREEN + seconds + "\n" +
                    ChatColor.YELLOW + "Group means: only specific areas, like specifically 16 or 32 blocks\n" +
                    ChatColor.GOLD + "===== NEW GROUP PERSONAL BEST =====");
            stats.setSpecificPB(areaGroup, secondsTime);
        }



        stats.updateAverageTime(secondsTime);
    }
    public void stopTimerFail2() {
        timer = false;
        p.teleport(spawnpoint);
        clearBlocks();
        p.sendMessage(ChatColor.RED + "You failed while bridging!");
        stats.addCoins(giveCoinsAmount(false, false));
        stats.addPlacedBlocks(lastPlacedBlocks.size());
        stats.setFailedAttempts(stats.getFailedAttempts() + 1);
    }
    public void stopTimerHidden() {
        timer = false;
        p.teleport(spawnpoint);
        clearBlocks();
        stats.addCoins(giveCoinsAmount(false, false));
        stats.addPlacedBlocks(lastPlacedBlocks.size());
    }
    private int giveCoinsAmount(boolean finished, boolean tooMuchTime) {
        return (int) Math.min(lastPlacedBlocks.size() * (finished ? 1.5 : (tooMuchTime ? 0.75 : 0.25)), finished ? 60 : (tooMuchTime ? 30 : 10));
    }
    private void stopTimerFail() {
        timer = false;
        p.teleport(spawnpoint);
        clearBlocks();
        p.sendMessage(ChatColor.RED + "You spent too much time bridging!");
        stats.addCoins(giveCoinsAmount(false, true));
        stats.addPlacedBlocks(lastPlacedBlocks.size());
        stats.setFailedAttempts(stats.getFailedAttempts() + 1);
    }
    private void clearBlocks() {
        List<Block> blocks = new ArrayList<>(placedBlocks);
        Bukkit.getScheduler().scheduleSyncDelayedTask(manager.getPlugin(), () -> recursiveClearBlock(blocks, 0), 10L);
        lastPlacedBlocks.clear();
        lastPlacedBlocks.addAll(placedBlocks);
        placedBlocks.clear();
    }
    private void recursiveClearBlock(List<Block> blocks, int index) {
        if(index >= blocks.size()) return;
        blocks.get(index).breakNaturally();
        Bukkit.getScheduler().scheduleSyncDelayedTask(manager.getPlugin(), () -> recursiveClearBlock(blocks, index + 1), 3L);
    }
    public int getBlockAmount() {
        if(placedBlocks.isEmpty()) return lastPlacedBlocks.size();
        return placedBlocks.size();
    }
    public String getAreaName() {
        return areaName;
    }

    public ProtectedRegion getAreaRegion() {
        return areaGuard;
    }

    private Player p;
    private BridgingStats stats;
    private GameMode gameMode;
    private Location lastLocation;
    // Last inv
    private ItemStack[] armorContents;
    private ItemStack[] invContents;
    //
    public void join(Player p) {
        if(available && !closed) {
            available = false;
            this.p = p;
            lastLocation = p.getLocation().clone();
            armorContents = p.getInventory().getArmorContents();
            invContents = p.getInventory().getContents();
            if(manager.gameMode != null) gameMode = p.getGameMode();
            stats = manager.getStats(p.getUniqueId());
            p.teleport(spawnpoint);
            if(manager.gameMode != null) p.setGameMode(manager.gameMode);
            ItemStack[] empty = new ItemStack[p.getInventory().getArmorContents().length];
            p.getInventory().setArmorContents(empty);
            p.getInventory().clear();
            p.getInventory().setItem(0, manager.getBlock(p));
            p.getInventory().setItem(7, getBlockSelector());
            p.getInventory().setItem(8, getLeaveItem());
            p.sendMessage(ChatColor.GREEN + "You joined area " + ChatColor.DARK_GREEN + areaName);
            manager.showScoreboard(p);
            timer = false;
            timerTicks = 0;
        }else throw new IllegalStateException("Area not available!");
    }

    public Player getPlayer() {
        return p;
    }

    public BridgingStats getStats() {
        return stats;
    }

    public void leave() {
        if(available || p == null) return;
        if(timer) stopTimerHidden();
        if(gameMode != null) p.setGameMode(gameMode);
        if(manager.teleportBack && lastLocation != null) p.teleport(lastLocation);
        else p.teleport(manager.getLobbyLocation());
        if(manager.keepInv && invContents != null) {
            p.getInventory().setContents(invContents);
            if(armorContents != null) p.getInventory().setArmorContents(armorContents);
        }else p.getInventory().clear();
        p.sendMessage(ChatColor.GREEN + "You left the area");
        manager.hideScoreboard(p);
        p = null;
        stats = null;
        gameMode = null;
        lastLocation = null;
        invContents = armorContents = null;
        available = true;
        timer = false;
    }
    private ItemStack getBlockSelector() {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Block Selector");
        item.setItemMeta(meta);
        return item;
    }
    private ItemStack getLeaveItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Leave Bridging");
        item.setItemMeta(meta);
        return item;
    }

    public boolean isAvailable() {
        return available;
    }
    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        if(p != null && e.getPlayer().getUniqueId() == p.getUniqueId()) {
            if(timer) {
                stats.addCoins(giveCoinsAmount(false, false));
                stats.addPlacedBlocks(placedBlocks.size());
            }
            leave();
        }
    }
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if(p == null || !(e.getPlayer().getUniqueId().equals(p.getUniqueId()))) return;
        if(!areaGuard.contains(e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ())) e.setCancelled(true);
        else if(e.getPlayer().getUniqueId().equals(p.getUniqueId())) {
            p.getInventory().getItemInHand().setAmount(e.getItemInHand().getMaxStackSize());
            placedBlocks.add(e.getBlock());
            if(!timer) startTimer();
        }

    }
    @EventHandler
    public void onMultiPlace(BlockMultiPlaceEvent e) {
        if(p == null || !(e.getPlayer().getUniqueId().equals(p.getUniqueId()))) return;
        for(BlockState state : e.getReplacedBlockStates()) {
            if(!areaGuard.contains(state.getX(), state.getY(), state.getZ())) {
                e.setCancelled(true);
                break;
            }
        }
        if(!e.isCancelled()) {
            for(BlockState state : e.getReplacedBlockStates()) {
                placedBlocks.add(state.getBlock());
            }
            p.getInventory().getItemInHand().setAmount(e.getItemInHand().getMaxStackSize());
        }
    }
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if(p != null && e.getPlayer().getUniqueId().equals(p.getUniqueId())) e.setCancelled(true);
    }
    @EventHandler
    public void onExitRegion(PlayerExitRegionEvent e) {
        if(p != null && e.getPlayer().getUniqueId().equals(this.p.getUniqueId()) && e.hasRegion(areaGuard.getId())) {
//            e.setCancelled(true);
            if(timer) stopTimerFail2();
            else p.teleport(spawnpoint);
        }
    }
    @EventHandler
    public void onEnterRegion(PlayerEnterRegionEvent e) {
        if(p != null && p.getUniqueId().equals(e.getPlayer().getUniqueId()) && e.hasRegion(finishLine.getId())) {
            stopTimer();
        }
    }
    @EventHandler
    public void onDamaged(EntityDamageEvent e) {
        if(p != null && e.getEntity() instanceof Player && p.getUniqueId().equals(e.getEntity().getUniqueId())) {
            e.setCancelled(true);
        }
    }

}
