package me.bramar.bedwarspractice.bridging;

import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.bramar.bedwarspractice.BedwarsPractice;
import me.bramar.bedwarspractice.bridging.blockselector.BlockSelectorTrait;
import me.bramar.bedwarspractice.bridging.blockselector.BridgingBlock;
import me.bramar.bedwarspractice.utils.ModuleLogger;
import me.bramar.bedwarspractice.utils.pagedinv.InventoryListener;
import me.bramar.bedwarspractice.utils.pagedinv.PagedAction;
import me.bramar.bedwarspractice.utils.pagedinv.PagedInventory;
import me.clip.placeholderapi.PlaceholderAPI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.Math.floor;

public final class BridgingManager implements Listener {
    boolean updated = false; // Was config updated?
    private final List<BridgingArea> areas;
    private Location lobby;
    private final BedwarsPractice main;
    public final YamlConfiguration config;
    private final Map<UUID, BridgingScoreboardTask> scoreboards = new HashMap<>();
    public final Logger logger;
    // Scoreboard
    private List<List<String>> contents;
    private List<String> titles;
    private boolean hasPlaceholder;
    // Stats
    private final Map<UUID, BridgingStats> stats = new HashMap<>();
    // Config
    boolean keepInv = true;
    GameMode gameMode = GameMode.SURVIVAL;
    boolean teleportBack = false;
    Map<String, List<String>> areaGroups = new HashMap<>();
    //
    public BridgingManager() {
        areas = new ArrayList<>();
        main = JavaPlugin.getPlugin(BedwarsPractice.class);
        main.saveResource("bridging.yml", false);
        configFile = new File(main.getDataFolder(), "bridging.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
        logger = new ModuleLogger("BedwarsPractice/Bridging");
        Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {
            this.load();
            if(hasPlaceholder) {
                main.getPlaceholders().register(new BridgingPlaceholders(this));
                logger.info("Loaded bridging placeholders");
            }
        }, 1);
        Bukkit.getPluginManager().registerEvents(this, getPlugin());
        try {
            if(Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
                CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(BlockSelectorTrait.class).withName("BlockSelector"));
                logger.info("Loaded BlockSelector citizens (NPC) trait");
            }
        }catch(Exception e1) {
            logger.log(Level.WARNING, "Failed to load BlockSelector citizens trait.", e1);
        }
    }


    /*
    Stats
     */

    public BridgingBlock getBlock(String id) {
        for(BridgingBlock b : blocks) {
            if(id.equalsIgnoreCase(b.getId())) return b;
        }
        return null;
    }
    private ItemStack getDefaultBlock(Player p) {
        String str = config.getString("default_block");
        if(str != null) {
            BridgingBlock b = getBlock(str);
            if(b != null) return b.getItem(p);
        }
        return new ItemStack(Material.STONE, 64);
    }

    public ItemStack getBlock(Player p) {
        String id = getStats(p.getUniqueId()).getChosenBlock();
        if(id == null) return getDefaultBlock(p);
        BridgingBlock b = getBlock(id);
        if(b == null) return getDefaultBlock(p);
        if(b.isDisabled() || (b.getPermission() != null && !p.hasPermission(b.getPermission()))) {
            BridgingStats stats = getStats(p.getUniqueId());
            stats.setChosenBlock("none");
            return getDefaultBlock(p);
        }
        return b.getItem(p);
    }

    public @Nullable BridgingStats getStatsWithoutDefault(UUID uuid) {
        return this.stats.get(uuid);
    }

    public Map.Entry<BridgingStats, String> getStats(String input) {
        BridgingStats stats;
        String name;
        if(BridgingStats.UUID_CHECK.matcher(input).matches()) {
            // UUID
            UUID uuid = UUID.fromString(input);
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            stats = main.getBridgingManager().getStats(uuid);
            name = player.getName();
        }else {
            name = input;
            OfflinePlayer player = Bukkit.getOfflinePlayer(input);
            stats = main.getBridgingManager().getStatsWithoutDefault(player.getUniqueId());
        }
        if(stats == null) return new KeyValue<>(null, null);
        return new KeyValue<>(stats, name);
    }

    @Contract("null -> null")
    public BridgingStats getStats(UUID uuid) {
        if(uuid == null) return null;
        try {
            BridgingStats stats = this.stats.get(uuid);
            if(stats != null) return stats;
        }catch(Exception ignored) {}
        // Create an empty one for stats
        ConfigurationSection section = config.createSection("stats." + uuid);
        section.set("average_time", 0);
        section.set("average_time_top", 0);
        section.set("personal_best", 0);
        section.set("personal_best_top", 0);
        section.set("blocks_placed", 0);
        section.set("coins", 0);
        section.set("coins_top", 0);
        section.set("bridge_count", 0);
        section.set("chosen_block", "none");
        section.set("owned_blocks", new ArrayList<>());
        section.set("failed_attempts", 0);
        updated = true;
        BridgingStats stats = new BridgingStats(this, uuid.toString());
        this.stats.put(uuid, stats);
        logger.info("Loaded BridgingStats of UUID " + uuid + " (from empty)");
        return stats;
    }

    final File configFile;

    public void saveConfig() {
        Bukkit.getScheduler().scheduleSyncDelayedTask(main, this::saveConfigNow, 1L);
    }
    public void saveConfigNow() {
        try {
            config.save(configFile);
            logger.log(Level.FINE, "Saved bridging.yml");
        }catch(IOException e) {
            logger.log(Level.SEVERE, "Failed to save config (bridging.yml)! There may be unsaved changes.", e);
        }
    }

    public Map<UUID, BridgingStats> getAllStats() {
        return Collections.unmodifiableMap(stats);
    }

    /*
    Scoreboard
     */
    public void showScoreboard(Player p) {
        if(!config.getBoolean("scoreboard.enabled")) return;
        if(scoreboards.containsKey(p.getUniqueId())) hideScoreboard(p);
        if(config.getBoolean("scoreboard.animated")) {
            BridgingScoreboard sb = new BridgingScoreboard(this, p,
                    contents, titles);

            BridgingScoreboardTask task = new BridgingScoreboardTask(sb, true);
            task.runTaskTimer(main, 3, 3);
            p.setScoreboard(sb.getScoreboard());
            scoreboards.put(p.getUniqueId(), task);
        }else {
            BridgingScoreboard sb = new BridgingScoreboard(this, p);
            BridgingScoreboardTask task = new BridgingScoreboardTask(sb, false);
            task.runTaskTimer(main, 5, 5);
            p.setScoreboard(sb.getScoreboard());
            scoreboards.put(p.getUniqueId(), task);
        }
    }
    public void hideScoreboard(Player p) {
        if(scoreboards.containsKey(p.getUniqueId())) {
            BridgingScoreboardTask task = scoreboards.get(p.getUniqueId());
            task.cancel();
            scoreboards.remove(p.getUniqueId());
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(main, () -> p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard()), 20L);
        }
    }
    public void hideScoreboardAll() {
        this.scoreboards.keySet().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if(p != null) hideScoreboard(p);
        });
    }
    public String getScoreboardTitle(Player p) {
        String s = ChatColor.translateAlternateColorCodes('&', config.getString("scoreboard.title", ""));
        return (hasPlaceholder) ? PlaceholderAPI.setPlaceholders(p, s) : s;
    }
    /*
    Bridging Areas
     */

    public String getAreaGroup(String areaName) {
        for(Map.Entry<String, List<String>> entry : areaGroups.entrySet()) {
            if(entry.getValue().contains(areaName))
                return entry.getKey();
        }
        areaGroups.get("default").add(areaName);
        saveConfig();
        return "default";
    }
    public void setAreaGroup(String areaName, String groupName) {
        if(getAreaGroup(areaName).equals(groupName)) return;
        for(Map.Entry<String, List<String>> entry : areaGroups.entrySet()) {
            // First: if the group is the one to set, add it to the list
            // Second: If its not the one to set AND it is the one the area is in, remove it from the list
            if(entry.getKey().equals(groupName))
                entry.getValue().add(areaName);
            else if(entry.getValue().contains(areaName))
                entry.getValue().remove(areaName);
        }
        for(Map.Entry<String, List<String>> entry : areaGroups.entrySet()) {
            if(entry.getValue().isEmpty() && !entry.getKey().equals("default"))
                areaGroups.remove(entry.getKey());
        }
        saveConfig();
    }

    public Map<String, List<String>> getAreaGroups() {
        return areaGroups;
    }

    public @Nullable BridgingArea getArea(Player p) {
        final UUID uuid = p.getUniqueId();
        if(uuid != null) try {
            return areas.stream().filter((b) -> b != null && b.getPlayer() != null && uuid.equals(b.getPlayer().getUniqueId())).findFirst().orElse(null);
        }catch(Exception ignored) {}
        return null;
    }
    public void join(Player p) {
        for(BridgingArea area : areas) {
            if(area.isAvailable() && !area.isClosed()) {
                area.join(p);
                return;
            }
        }
        throw new RuntimeException("There is currently no bridging area available! (All filled)");
    }
    public @Nullable BridgingArea getArea(String areaName) {
        return areas.stream().filter((a) -> areaName.equalsIgnoreCase(a.getAreaName())).findFirst().orElse(null);
    }
    public void setAreaFlags(ProtectedRegion region) {
        region.setFlag(DefaultFlag.BLOCK_BREAK, StateFlag.State.ALLOW);
        region.setFlag(DefaultFlag.BLOCK_PLACE, StateFlag.State.ALLOW);
        region.setFlag(DefaultFlag.BUILD, StateFlag.State.ALLOW);
        region.setFlag(DefaultFlag.ENDERPEARL, StateFlag.State.DENY);
        region.setFlag(DefaultFlag.TNT, StateFlag.State.DENY);
        region.setFlag(DefaultFlag.PVP, StateFlag.State.DENY);
        region.setFlag(DefaultFlag.INVINCIBILITY, StateFlag.State.ALLOW);
        region.setFlag(DefaultFlag.ENDER_BUILD, StateFlag.State.DENY);
        region.setFlag(DefaultFlag.CREEPER_EXPLOSION, StateFlag.State.DENY);
        region.setFlag(DefaultFlag.OTHER_EXPLOSION, StateFlag.State.DENY);
        region.setFlag(DefaultFlag.FIRE_SPREAD, StateFlag.State.DENY);
        region.setFlag(DefaultFlag.MIN_FOOD, 20);
        region.setFlag(DefaultFlag.MOB_SPAWNING, StateFlag.State.DENY);
        region.setFlag(DefaultFlag.ITEM_DROP, StateFlag.State.DENY);
    }

    /*
    Loading BridgingManager and areas
     */

    private boolean wasLoaded = false;
    private int configTask = 0;

    public void load() {
        if(wasLoaded) {
            Bukkit.getScheduler().cancelTask(configTask);
            configTask = -1;
        }else wasLoaded = true;
        ConfigurationSection ls = config.getConfigurationSection("lobby");
        World world = Bukkit.getWorld(ls.getString("world"));
        Location loc = new Location(world, ls.getDouble("x"), ls.getDouble("y"), ls.getDouble("z"));
        if(ls.contains("yaw")) loc.setYaw((float) ls.getDouble("yaw"));
        if(ls.contains("pitch")) loc.setPitch((float) ls.getDouble("pitch"));
        lobby = loc;
        if(world != null)
            logger.info("Loaded lobby spawnpoint: " + xyzFromLocation(loc));
        else
            logger.warning("Lobby spawnpoint has been set but the world is invalid/not loaded. Load the world then reload Bridging to prevent errors.");

        // Area groups
        ConfigurationSection groups = config.getConfigurationSection("area_groups");
        if(groups != null) {
            // get each group as a List of strings
            for(String group : groups.getKeys(false)) {
                List<String> areaNames = groups.getStringList(group);
                areaGroups.put(group, areaNames);
            }
            areaGroups.put("default", new ArrayList<>());
        }else
            logger.warning("Area groups are not defined so all personal bests will be global.");

        // Scoreboard
        ConfigurationSection sbConfig = config.getConfigurationSection("scoreboard");
        this.contents = null;
        this.titles = null;
        if(sbConfig.getBoolean("animated")) {
            ConfigurationSection contentsConfig = sbConfig.getConfigurationSection("contents");
            ArrayList<ObjectOrders<List<String>>> contents = new ArrayList<>();
            ArrayList<ObjectOrders<String>> titles = new ArrayList<>();
            List<String> mainContent = sbConfig.getStringList("content");
            for(String key : contentsConfig.getKeys(false)) {
                if(contentsConfig.isConfigurationSection(key)) {
                    try {
                        int i = Integer.parseInt(key);
                        ConfigurationSection section = contentsConfig.getConfigurationSection(key);
                        titles.add(new ObjectOrders<>(section.getString("title"), i));
                        if(section.getBoolean("main_content", false))
                            contents.add(new ObjectOrders<>(mainContent, i));
                        else
                            contents.add(new ObjectOrders<>(section.getStringList("content"), i));
                    }catch(Exception ignored) {}
                }
            }
            this.contents = contents.stream().sorted(Comparator.comparingInt(o -> o.order))
                    .map(o1 -> o1.obj).collect(Collectors.toList());
            this.titles = titles.stream().sorted(Comparator.comparingInt(o -> o.order))
                    .map(o1 -> o1.obj).collect(Collectors.toList());
        }
        logger.info("Loaded scoreboard");
        // Stats
        ConfigurationSection stats = config.getConfigurationSection("stats");
        this.stats.clear();
        for(String key : stats.getKeys(false)) {
            if(stats.isConfigurationSection(key)) {
                try {
                    BridgingStats stat = new BridgingStats(this, key);
                    this.stats.put(stat.getUniqueId(), stat);
                }catch(Exception ignored) {}
            }
        }
        logger.info("Loaded player statistics");
        // Blocks
        ConfigurationSection shop = config.getConfigurationSection("shop");
        blocks.clear();
        main_loop:
        for(String key : shop.getKeys(false)) {
            if(shop.isConfigurationSection(key)) {
                try {
                    ConfigurationSection section = shop.getConfigurationSection(key);
                    // Check duplicates
                    String id = section.getString("id", null);
                    if(id == null) continue;
                    for(BridgingBlock b : blocks) {
                        if(b.getId().equalsIgnoreCase(id)) continue main_loop;
                    }
                    //
                    blocks.add(new BridgingBlock(this, section));
                }catch(Exception ignored) {}
            }
        }
        logger.info("Loaded bridging shop");
        // Other config
        keepInv = config.getBoolean("keep-inventory", true);
        teleportBack = config.getBoolean("teleport-back", false);
        String gameModeStr = config.getString("gamemode");
        try {
            if(gameModeStr != null) {
                if(gameModeStr.equalsIgnoreCase("none")) gameMode = null;
                else gameMode = GameMode.valueOf(gameModeStr.toUpperCase());
            }
        }catch(Exception e1) {
            logger.warning("Failed to load GameMode value for 'gamemode' for input: " + gameModeStr);
        }

        // Automatic Config Save
        configTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(main, () -> {
            if(updated) {
                saveConfig();
                updated = false;
            }
        }, 10 * 20, 10 * 20); // Every 10 sec if updated
        logger.info("Created automatic config save task");
        //
        hasPlaceholder = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        loadAreas();
    }

    private void loadAreas() {
        ConfigurationSection areas = config.getConfigurationSection("areas");
        if(areas != null) for(String key : areas.getKeys(false)) {
            if(areas.isConfigurationSection(key)) this.areas.add(new BridgingArea(this, areas.getConfigurationSection(key)));
        }
    }

    /* Get methods */

    public List<BridgingArea> getAreas() {
        return areas;
    }
    public Location getLobbyLocation() {
        return lobby;
    }
    public BedwarsPractice getPlugin() {
        return main;
    }
    public boolean hasPlaceholderAPI() {
        return hasPlaceholder;
    }

    /* Events */

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = e.getItem();
            if(getArea(e.getPlayer()) != null && item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if(meta != null && meta.getDisplayName() != null) {
                    if(meta.getDisplayName().equalsIgnoreCase(ChatColor.GOLD + "Block Selector")) openBlockSelector(e.getPlayer());
                    if(meta.getDisplayName().equalsIgnoreCase(ChatColor.RED + "Leave Bridging")) e.getPlayer().performCommand("bridging leave");
                    else return;
                    e.setCancelled(true);
                }
            }
        }
    }
    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        hideScoreboard(e.getPlayer());
    }
    /* Block Selector */

    private final List<BridgingBlock> blocks = new ArrayList<>();
    public void openBlockSelector(Player p) {
        List<ItemStack> listAll = new ArrayList<>();
        for(BridgingBlock b : blocks) {
            listAll.add(b.getShopItem(p));
        }
        List<List<ItemStack>> list = new ArrayList<>();
        int limit = 9*2-1;
        while(listAll.size() > limit)
            list.add((listAll = listAll.subList(0, limit)));
        if(listAll.size() > 0) list.add(listAll);
        ItemStack[][] content = new ItemStack[list.size()][];
        for(int a = 0; a < list.size(); a++) {
            List<ItemStack> items = list.get(a);
            ItemStack[] itemArr = new ItemStack[items.size()];
            for(int i = 0; i < items.size(); i++) itemArr[i] = items.get(i);
            content[a] = itemArr;
        }
        p.openInventory(
                PagedInventory.create(
                        "&6Block Selector GUI",
                        9*4,
                        content.length,
                        1,
                        content,
                        new BlockSelectorListener()
                )
        );
    }

    // Util methods

    private String xyzFromLocation(Location loc) {
        return "[" + floor(loc.getX()) + ", " + floor(loc.getY()) + ", " + floor(loc.getZ()) + "]";
    }


    /*
    Custom private classes
     */

    private class BlockSelectorListener implements InventoryListener {
        @Override
        public void run(@NotNull InventoryClickEvent e, int currentPage, int maxPage, @NotNull PagedAction action) {
            try {
                if(action == PagedAction.NONE && e.getWhoClicked() instanceof Player && e.getCurrentItem() != null) {
                    Player p = (Player) e.getWhoClicked();
                    NBTTagCompound tag = PagedInventory.getNBTTag(e.getCurrentItem());
                    String id = tag.getString("block_id");
                    if(id != null) {
                        BridgingBlock block = getBlock(id);
                        p.closeInventory();
                        if(block == null) p.sendMessage(ChatColor.RED + "BridgingBlock not found! Try again.");
                        else {
                            if(block.isDisabled()) p.sendMessage(ChatColor.RED + "This BridgingBlock is currently disabled!");
                            else {
                                BridgingStats stats = getStats(p.getUniqueId());
                                if(block.getPermission() != null && !p.hasPermission(block.getPermission()))
                                    p.sendMessage(ChatColor.RED + "You do not have the permission to use the BridgingBlock!");
                                else if(stats.ownBlock(id) || block.isDefault()) {
                                    p.getInventory().setItem(0, block.getItem(p));
                                    p.sendMessage(ChatColor.GREEN + "You have equipped " + id + " block");
                                    p.playSound(p.getLocation(), Sound.LEVEL_UP, 1, 1);
                                    stats.setChosenBlock(id);
                                }else {
                                    if(stats.getCoins() >= block.getPrice()) {
                                        stats.addCoins(-block.getPrice());
                                        stats.addOwnedBlock(id);
                                        p.sendMessage(ChatColor.YELLOW + "You bought " + id + " block for " + block.getPrice() + " coins!");
                                        p.playSound(p.getLocation(), Sound.LEVEL_UP, 1, 1.9f);
                                    }else p.sendMessage(ChatColor.RED + "You do not have enough coins to buy the block! [" + block.getPrice() + "]");
                                }
                            }
                        }
                    }
                }
            }catch(Exception ignored) {}
        }
    }

    private static class KeyValue<K, V> implements Map.Entry<K, V> {
        final K obj1;
        V obj2;
        KeyValue(K obj, V obj2) {
            this.obj1 = obj;
            this.obj2 = obj2;
        }

        @Override
        public K getKey() {
            return obj1;
        }

        @Override
        public V getValue() {
            return obj2;
        }

        @Override
        public V setValue(V value) {
            V oldValue = obj2;
            obj2 = value;
            return oldValue;
        }
    }
    // Just a simple wrapper class for order
    private static class ObjectOrders<T> {
        T obj;
        int order;
        ObjectOrders(T obj, int order) {
            this.obj = obj;
            this.order = order;
        }
    }
}

