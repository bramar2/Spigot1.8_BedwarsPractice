package me.bramar.bedwarspractice.bridging;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.bramar.bedwarspractice.BPCommand;
import me.bramar.bedwarspractice.BedwarsPractice;
import me.bramar.bedwarspractice.bridging.blockselector.BlockSelectorTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.SpawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BridgingCommand extends BPCommand {
    private final BedwarsPractice main;
    private final BridgingManager manager;

    @Override
    public String getCommand() {
        return "bedwarsbridging";
    }

    @Override
    public String getName() {
        return "bridging";
    }

    private List<String> helpConsole;

    @Override
    public List<String> getHelp() {
        if(helpConsole != null) return helpConsole;
        List<String> list = Arrays.asList(
                "/bridging stats <player>: Shows that player's statistics",
                "/bridging kick <area>: Kicks the player in the area (if occupied)",
                "/bridging forceplay <player>: Forces the player to join an area",
                "/bridging forcejoin <player> <area>: Forces the player to join the (specified) area",
                "/bridging close <area>: Closes the area (kicks the player if occupied)",
                "/bridging open <area>: Opens the area if its closed",
                "/bridging areas: Shows all areas with status",
                "/bridging clearstat <player> <stat|all>: Clears that player's specified stat",
                "/bridging clearstats <player>: Clears that player's statistics.\n   Identical to /bridging clearstat <player> <all>",
                "/bridging getlb <coins|average|pb> <number> [number_to]: Type without any args for help",
                "/bridging setstat <player> <stat> <value>: Sets that player's stat with the value. Error if type is incompatible with input"
        );
        return helpConsole = alternatingColors(list, ChatColor.AQUA, ChatColor.DARK_AQUA);
    }

    @Override
    public String getPermissionMessage() {
        return ChatColor.RED + "No permission!";
    }

    private List<String> helpUser;
    private List<String> helpAdmin;

    @Override
    public @Nullable List<String> getHelp(Player p) {
        boolean isAdmin = p.hasPermission("bedwarspractice.bridging.adminhelp");
        if(helpUser != null && !isAdmin) return helpUser;
        if(helpAdmin != null && isAdmin) return helpAdmin;
        List<String> args = new ArrayList<>(Arrays.asList(
                "/bridging play: Joins any available area",
                "/bridging join <area>: Joins that area (if available)",
                "/bridging leave: Leaves the area (if you're playing)",
                "/bridging stats: Tells you your statistics while playing Bedwars Bridging"
        ));
        args = alternatingColors(args, ChatColor.AQUA, ChatColor.DARK_AQUA);
        helpUser = args;
        helpAdmin = new ArrayList<>(args);
        helpAdmin.add(ChatColor.GREEN + "/bridging adminhelp: Tells you every admin command");
        return (isAdmin) ? helpAdmin : helpUser;
    }

    @Override
    public @NotNull List<String> validArguments() {
        return Arrays.asList("play", "join", "leave", "stats", "adminhelp",
                "kick", "forceplay", "forcejoin", "close", "open", "areas",
                "create", "delete", "setspawn", "setarea", "setfinish", "setlobby",
                "blocknpc", "clearstat", "clearstats", "getlb", "reload", "setstat", "setgroup");
    }

    @Override
    public void onConsoleCommand(ConsoleCommandSender p, String mainArg, String[] args) throws Exception {
        switch(mainArg.toLowerCase()) {
            case "stats":
                if(true) {
                    if(args.length < 2) {
                        p.sendMessage(ChatColor.RED + "Usage: /bridging stats <player>: Shows that player's statistics");
                        break;
                    }
                    UUID uuid;
                    String name = null;
                    if(BridgingStats.UUID_CHECK.matcher(args[1]).matches()) uuid = UUID.fromString(args[1]);
                    else {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
                        uuid = player.getUniqueId();
                        name = player.getName();
                    }
                    if(name == null) name = Bukkit.getOfflinePlayer(uuid).getName();
                    BridgingStats stats = manager.getStatsWithoutDefault(uuid);
                    if(stats == null) throw new NullPointerException(name + " does not have BridgingStats (or the player does not exist)");
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            String.format(
                                    "&a" + name + "'s bridging stats:\n" +
                                            "&bPlaced Blocks - &a%s\n" +
                                            "&bFinished bridges - &a%s\n" +
                                            "&cFailed attempts - &a%s\n" +
                                            "&eCoins - %s\n\n" +
                                            "&3Average time - &a%ss\n" +
                                            "&6Personal Best - &a%s\n\n" +
                                            "&3Average time Rank - &6#%s/%s\n" +
                                            "&6Personal best Rank - &6#%s/%s\n" +
                                            "&eMost coins Rank - &6#%s/%s",
                                    stats.getBlocksPlaced(),
                                    stats.getBridgeCount(),
                                    stats.getFailedAttempts(),
                                    stats.getCoins(),
                                    stats.getAverageTime(),
                                    stats.getPersonalBest(),
                                    stats.getAverageTimeTop(),
                                    BridgingStats.getTopAverageTime().size(),
                                    stats.getPersonalBestTop(),
                                    BridgingStats.getTopPB().size(),
                                    stats.getCoinsTop(),
                                    BridgingStats.getTopCoins().size()
                            )
                    ));
                    stats.updateTop();
                }
                break;
            case "kick":
                if(args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging kick <player>");
                }else {
                    Player player = Bukkit.getPlayer(args[1]);
                    if(player == null) throw new NullPointerException("Player not found!");
                    BridgingArea area = manager.getArea(player);
                    if(area == null) throw new NullPointerException("That player is not in a bridging area");
                    area.leave();
                    player.sendMessage(ChatColor.RED + "You have been force-kicked from the area!");
                    p.sendMessage(ChatColor.GREEN + "Successfully force-kicked " + player.getName());
                }
                break;
            case "forceplay":
                if(args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging forceplay <player>");
                }else {
                    Player player = Bukkit.getPlayer(args[1]);
                    if(player == null) throw new NullPointerException("Player not found!");
                    BridgingArea currentArea = manager.getArea(player);
                    if(currentArea != null) currentArea.leave();
                    manager.join(player);
                    p.sendMessage(ChatColor.GREEN + "Successfully force-played " + player.getName());
                }
                break;
            case "forcejoin":
                if(args.length < 3) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging forcejoin <player> <area>");
                }else {
                    Player player = Bukkit.getPlayer(args[1]);
                    BridgingArea currentArea = manager.getArea(player);
                    if(currentArea != null) currentArea.leave();
                    BridgingArea area = manager.getArea(args[2]);
                    if(area == null) throw new NullPointerException("Area not found!");
                    if(!area.isAvailable()) throw new NullPointerException("Area is not available/occupied!");
                    if(area.isClosed()) throw new IllegalStateException("Area is currently closed!");
                    area.join(player);
                    p.sendMessage(ChatColor.GREEN + "Successfully force-joined " + player.getName());
                }
                break;
            case "close":
                if(args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging close <area>");
                }else {
                    BridgingArea area = manager.getArea(args[1]);
                    if(area == null) throw new NullPointerException("Area not found!");
                    if(area.isClosed()) {
                        p.sendMessage(ChatColor.RED + "That area is already closed!");
                    }else {
                        if(!area.isAvailable()) {
                            Player player = area.getPlayer();
                            area.leave();
                            player.sendMessage(ChatColor.RED + "The area you were in has been closed!");
                        }
                        area.close();
                        p.sendMessage(ChatColor.GREEN + "Successfully closed the area!");
                    }
                }
                break;
            case "open":
                if(args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging open <area>");
                }else {
                    BridgingArea area = manager.getArea(args[1]);
                    if(area == null) throw new NullPointerException("Area not found!");
                    if(area.isClosed()) {
                        area.open();
                        p.sendMessage(ChatColor.GREEN + "Successfully opened the area!");
                    }else p.sendMessage(ChatColor.RED + "That area is already opened!");
                }
                break;
            case "areas":
                if(true) {
                    List<BridgingArea> areas = manager.getAreas();
                    StringBuilder msg = new StringBuilder("&6Areas (&a" + areas.size() + "&6): ");
                    for(int i = 0; i < areas.size(); i++) {
                        BridgingArea area = areas.get(i);
                        msg.append((area.isClosed()) ? "&7" + area.getAreaName() + " (closed)" :
                                (!area.isAvailable() && area.getPlayer() != null) ? "&c" + area.getAreaName() + " (occupied by &7" + area.getPlayer().getName() + ")" :
                                (area.isAvailable()) ? "&a" + area.getAreaName() + " (available)" : "&c" + area.getAreaName() + " (unavailable)");
                        if(i < areas.size() - 1) msg.append("&6, ");
                    }
                    p.sendMessage(
                            ChatColor.translateAlternateColorCodes('&', msg.toString())
                    );
                }
                break;
            case "clearstat":
                if(args.length < 3) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging clearstat <player> [stat|all]\n" +
                            "Stats available:\n" +
                            "average_time, personal_best, blocks_placed, coins, bridge_count, failed_attempts, owned_blocks");
                }else {
                    Map.Entry<BridgingStats, String> output = manager.getStats(args[1]);
                    BridgingStats stats = output.getKey();
                    String name = output.getValue();
                    if(stats == null) throw new NullPointerException("Player not found!");
                    Object value;
                    if(args[2].equalsIgnoreCase("all")) {
                        value = stats;
                        stats.resetAndSave();
                    }else if(args[2].equalsIgnoreCase("average_time"))
                        value = stats.resetAverageTime();
                    else if(args[2].equalsIgnoreCase("personal_best"))
                        value = stats.setPersonalBest(0.0d);
                    else if(args[2].equalsIgnoreCase("blocks_placed"))
                        value = stats.resetPlacedBlocks();
                    else if(args[2].equalsIgnoreCase("coins"))
                        value = stats.resetCoins();
                    else if(args[2].equalsIgnoreCase("bridge_count"))
                        value = stats.setBridgeCount(0);
                    else if(args[2].equalsIgnoreCase("failed_attempts"))
                        value = stats.setFailedAttempts(0);
                    else if(args[2].equalsIgnoreCase("owned_blocks")) {
                        value = new ArrayList<>(stats.getOwnedBlocks());
                        stats.getOwnedBlocks().clear();
                        manager.updated = true;
                    }
                    else throw new NullPointerException("Invalid stat!");
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&aSuccessfully cleared &6" + name + "&a's statistic of &6" + args[2] + "&a (previously " + value + ")"));
                }
                break;
            case "clearstats":
                if(args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging clearstats <player>");
                }else {
                    String name = args[1];
                    UUID uuid;
                    if(BridgingStats.UUID_CHECK.matcher(args[1]).matches()) {
                        UUID uuid2 = UUID.fromString(args[1]);
                        Player player = Bukkit.getPlayer(uuid2);
                        if(player == null) {
                            OfflinePlayer off = Bukkit.getPlayer(uuid2);
                            if(off == null) throw new NullPointerException("Player not found!");
                            name = off.getName();
                        }else name = player.getName();
                        uuid = uuid2;
                    }else {
                        Player player = Bukkit.getPlayer(name);
                        if(player == null) {
                            OfflinePlayer off = Bukkit.getPlayer(name);
                            if(off == null) throw new NullPointerException("Player not found!");
                            uuid = off.getUniqueId();
                        }else uuid = player.getUniqueId();
                    }
                    if(uuid == null) throw new NullPointerException("Player not found!");
                    BridgingStats stats = manager.getStatsWithoutDefault(uuid);
                    if(stats == null) throw new NullPointerException("Player does not have any statistics!");
                    stats.resetAndSave();
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&aSuccessfully resetted &6" + name + "&a's statistics!"));
                }
                break;
            case "getlb":
                if(args.length < 3) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging getlb <coins|average|pb> <number> [number_to]\n" +
                            "'coins' for most coins. 'average' for lowest average time. 'pb' for lowest personal best (time).\n" +
                            "number_to example:\n" +
                            "Without: Only the number\n" +
                            "With: From the number UNTIL [number_to] (like #1-50 lb)");
                }else {
                    int from, to;
                    from = to = Integer.parseInt(args[2]);
                    if(args.length > 3) to = Integer.parseInt(args[3]);
                    if(from < 1 || to < 1) throw new NumberFormatException("Number must be 1 or above");
                    if(to < from) throw new NumberFormatException("[number_to] must be bigger than original number.");
                    if(to - from + 1 > 50) throw new IllegalArgumentException("Only allowed max. 50 lb/command");
                    List<UUID> topList = args[1].equalsIgnoreCase("coins") ?
                            BridgingStats.getTopCoins() : (
                            args[1].equalsIgnoreCase("average") ?
                                    BridgingStats.getTopAverageTime() :
                                    (args[1].equalsIgnoreCase("pb") ?
                                            BridgingStats.getTopPB() :
                                            null)
                    );
                    if(topList == null) throw new IllegalArgumentException("Leaderboard type must either be: coins, average, or pb");
                    String endWithS = args[1].equalsIgnoreCase("coins") ? "" : "s";
                    ChatColor color = endWithS.isEmpty() ? ChatColor.YELLOW : (args[1].equalsIgnoreCase("average") ? ChatColor.AQUA : ChatColor.GOLD);
                    String str = "";
                    for(int i = from - 1; i < to; i++) {
                        if(topList.size() >= i + 1) {
                            UUID uuid = topList.get(i);
                            String name = uuid.toString();
                            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                            if(player != null) name = player.getName();
                            BridgingStats stats = manager.getStats(uuid);
                            String value = "" + (endWithS.isEmpty() ? stats.getCoins() :
                                    (color == ChatColor.AQUA ? stats.getAverageTime() : stats.getPersonalBest()));
                            str += ChatColor.translateAlternateColorCodes('&',
                                    "&5#" + (i + 1) + ". " + name + " [" + color + "" + value + "" + endWithS + "&5]");
                        }else {
                            if(from == to) str += ChatColor.GRAY + "#" + from + ". No one!";
                            else str += ChatColor.GRAY + "#" + (i + 1) + "-" + to + ". No one!\n";
                            break;
                        }
                    }
                    p.sendMessage(ChatColor.LIGHT_PURPLE + "Leaderboard of " + args[1] + " from #" + from + " to #" + to + ":\n" +
                            str);
                }
                break;
            case "setstat":
                if(args.length < 4) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging setstat <player> <stat> <value>\n" +
                            "All available stat:\n" +
                            "average_time, personal_best, blocks_placed, coins, bridge_count, failed_attempts");
                }else {
                    Map.Entry<BridgingStats, String> entry = manager.getStats(args[1]);
                    BridgingStats stats = entry.getKey();
                    String name = entry.getValue();
                    if(stats != null && name != null) {
                        String statName = args[2];
                        boolean a;
                        // Double
                        if((a = statName.equalsIgnoreCase("average_time")) || statName.equalsIgnoreCase("personal_best")) {
                            Double value = getDouble(args[3]);
                            if(value == null) throw new NumberFormatException("Invalid value. Stat type is double (a number with/out decimals). For type value: \"" + args[2] + "\"");
                            double old = a ? stats.getAverageTime() : stats.getPersonalBest();
                            if(a) stats.setAverageTime(value);
                            else stats.setPersonalBest(value);
                            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&aSuccessfully set &6" + name + "&a's stat of " +
                                            "&6" + statName + "&a to &6" + value + "&as (previously &6" + old + "&as)"));
                        }else {
                            BiConsumer<BridgingStats, Integer> set;
                            Function<BridgingStats, Integer> get;
                            if(statName.equalsIgnoreCase("blocks_placed")) {
                                set = BridgingStats::setPlacedBlocks;
                                get = BridgingStats::getBlocksPlaced;
                            }else if(statName.equalsIgnoreCase("coins")) {
                                set = BridgingStats::setCoins;
                                get = BridgingStats::getCoins;
                            }else if(statName.equalsIgnoreCase("bridge_count")) {
                                set = BridgingStats::setBridgeCount;
                                get = BridgingStats::getBridgeCount;
                            }else if(statName.equalsIgnoreCase("failed_attempts")) {
                                set = BridgingStats::setFailedAttempts;
                                get = BridgingStats::getFailedAttempts;
                            }else throw new IllegalArgumentException("Invalid stat name. Available stat names: average_time, personal_best, blocks_placed, coins, bridge_count, failed_attempts");
                            int value;
                            try {
                                value = Integer.parseInt(args[3]);
                            }catch(Exception e1) {
                                throw new NumberFormatException("Invalid value. Stat type is integer (a number without decimals). For type value: \"" + args[3] + "\"");
                            }
                            int old = get.apply(stats);
                            set.accept(stats, value);
                            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&aSuccessfully set &6" + name + "&a's stat of &6" +
                                            statName + "&a to &6" + value + " &a(previously &6" + old + "&a)"));
                        }
                    }
                }
                break;
            case "reload":
                manager.config.load(manager.configFile);
                p.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
                manager.getAreas().forEach(area -> {
                    if(area.getPlayer() != null) {
                        Player player = area.getPlayer();
                        if(player.isOnline()) {
                            area.leave();
                            player.sendMessage(ChatColor.RED + "Your server have rebooted BedwarsBridging. You have been kicked from your Bridging Area.");
                        }
                    }
                    HandlerList.unregisterAll(area); // Remove them as a Listener to events
                    Bukkit.getScheduler().cancelTask(area.repeatingTaskId);
                });
                p.sendMessage(ChatColor.RED + "Force-left all players in a bridging area");
                manager.hideScoreboardAll();
                p.sendMessage(ChatColor.RED + "Hid scoreboard for all players (in a bridging area)");
                manager.getAreas().clear();
                p.sendMessage(ChatColor.YELLOW + "Cleared all areas currently loaded!");
                manager.load();
                p.sendMessage(ChatColor.GREEN + "Successfully loaded Bedwars Bridging!");
                break;
            default:
                p.sendMessage("The argument you inputted is not supported in console");
        }
    }
    @Override
    public void onCommand(Player p, String mainArg, String[] args) throws Exception {
        switch(mainArg.toLowerCase()) {
            case "play":
                if(true) {
                    BridgingArea area = manager.getArea(p);
                    if(area != null) area.leave();
                    manager.join(p);
                    break;
                }
            case "join":
                if(args.length < 2) p.sendMessage(ChatColor.RED + "Usage: /bridging join <area>");
                else {
                    BridgingArea area2 = manager.getArea(p);
                    if(area2 != null) area2.leave();
                    BridgingArea area = manager.getArea(args[1]);
                    if(area == null) throw new NullPointerException("Area not found!");
                    if(!area.isAvailable()) throw new IllegalStateException("Area is not available!");
                    if(area.isClosed()) throw new IllegalStateException("Area is currently closed!");
                    area.join(p);
                }
                break;
            case "leave":
                if(true) {
                    BridgingArea area = manager.getArea(p);
                    if(area == null) throw new NullPointerException("You are currently not in a bridging area");
                    area.leave();
                    break;
                }
            case "stats":
                if(true) {
                    UUID uuid;
                    String name = null;
                    if(args.length == 1) {
                        uuid = p.getUniqueId();
                        name = p.getName();
                    }
                    else if(p.hasPermission("bedwarspractice.bridging.stats.other")) {
                        if(BridgingStats.UUID_CHECK.matcher(args[1]).matches()) uuid = UUID.fromString(args[1]);
                        else {
                            OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
                            uuid = player.getUniqueId();
                            name = player.getName();
                        }
                        if(name == null) name = Bukkit.getOfflinePlayer(uuid).getName();
                    }else throw new Exception("You don't have the permission to access other player's stats");
                    BridgingStats stats = manager.getStatsWithoutDefault(uuid);
                    if(stats == null) throw new NullPointerException(name + " does not have BridgingStats (or the player does not exist)");
                    StringBuilder groupedPBs = new StringBuilder();
                    Set<String> listedGroups = new HashSet<>();

                    for(Map.Entry<String, BridgingStats.PersonalBest> entry : stats.getSpecificPBs().entrySet()) {
                        String group = entry.getKey();
                        BridgingStats.PersonalBest pb = entry.getValue();

                        listedGroups.add(group);
                        groupedPBs.append(group).append(": ").append(pb.time <= 0.01d ? "-" : pb.time).append("\n");
                    }

                    for(String group : manager.getAreaGroups().keySet()) {
                        if(listedGroups.contains(group)) continue;

                        groupedPBs.append(group).append(": -\n");
                    }

                    p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            String.format(
                                    "&a" + name + "'s bridging stats:\n" +
                                            "&bPlaced Blocks - &a%s\n" +
                                            "&bFinished bridges - &a%s\n" +
                                            "&cFailed attempts - &a%s\n" +
                                            "&eCoins - %s\n\n" +
                                            "&3Average time - &a%ss\n" +
                                            "&6Global personal Best - &a%s\n\n" +
                                            "&3Average time Rank - &6#%s/%s\n" +
                                            "&6Global personal best Rank - &6#%s/%s\n" +
                                            "&eMost coins Rank - &6#%s/%s\n\n" +
                                            "&6Grouped personal bests:\n" +
                                            "&3%s" +
                                    stats.getBlocksPlaced(),
                                    stats.getBridgeCount(),
                                    stats.getFailedAttempts(),
                                    stats.getCoins(),
                                    stats.getAverageTime(),
                                    stats.getPersonalBest(),
                                    stats.getAverageTimeTop(),
                                    BridgingStats.getTopAverageTime().size(),
                                    stats.getPersonalBestTop(),
                                    BridgingStats.getTopPB().size(),
                                    stats.getCoinsTop(),
                                    BridgingStats.getTopCoins().size(),
                                    groupedPBs
                            )
                    ));
                    stats.updateTop();
                    break;
                }
            case "adminhelp":
                p.sendMessage(getAdminHelp());
                break;
            case "kick":
                if(args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging kick <player>");
                }else {
                    Player player = Bukkit.getPlayer(args[1]);
                    if(player == null) throw new NullPointerException("Player not found!");
                    BridgingArea area = manager.getArea(player);
                    if(area == null) throw new NullPointerException("That player is not in a bridging area");
                    area.leave();
                    player.sendMessage(ChatColor.RED + "You have been force-kicked from the area!");
                    p.sendMessage(ChatColor.GREEN + "Successfully force-kicked " + player.getName());
                }
                break;
            case "forceplay":
                if(args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging forceplay <player>");
                }else {
                    Player player = Bukkit.getPlayer(args[1]);
                    if(player == null) throw new NullPointerException("Player not found!");
                    BridgingArea currentArea = manager.getArea(player);
                    if(currentArea != null) currentArea.leave();
                    manager.join(player);
                    p.sendMessage(ChatColor.GREEN + "Successfully force-played " + player.getName());
                }
                break;
            case "forcejoin":
                if(args.length < 3) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging forcejoin <player> <area>");
                }else {
                    Player player = Bukkit.getPlayer(args[1]);
                    BridgingArea currentArea = manager.getArea(player);
                    if(currentArea != null) currentArea.leave();
                    BridgingArea area = manager.getArea(args[2]);
                    if(area == null) throw new NullPointerException("Area not found!");
                    if(!area.isAvailable()) throw new NullPointerException("Area is not available/occupied!");
                    if(area.isClosed()) throw new IllegalStateException("Area is currently closed!");
                    area.join(player);
                    p.sendMessage(ChatColor.GREEN + "Successfully force-joined " + player.getName());
                }
                break;
            case "close":
                if(args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging close <area>");
                }else {
                    BridgingArea area = manager.getArea(args[1]);
                    if(area == null) throw new NullPointerException("Area not found!");
                    if(area.isClosed()) {
                        p.sendMessage(ChatColor.RED + "That area is already closed!");
                    }else {
                        if(!area.isAvailable()) {
                            Player player = area.getPlayer();
                            area.leave();
                            player.sendMessage(ChatColor.RED + "The area you were in has been closed!");
                        }
                        area.close();
                        p.sendMessage(ChatColor.GREEN + "Successfully closed the area!");
                    }
                }
                break;
            case "open":
                if(args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging open <area>");
                }else {
                    BridgingArea area = manager.getArea(args[1]);
                    if(area == null) throw new NullPointerException("Area not found!");
                    if(area.isClosed()) {
                        area.open();
                        p.sendMessage(ChatColor.GREEN + "Successfully opened the area!");
                    }else p.sendMessage(ChatColor.RED + "That area is already opened!");
                }
                break;
            case "areas":
                List<BridgingArea> areas = manager.getAreas();
                TextComponent msg = new TextComponent(ChatColor.GOLD + "Areas (" + ChatColor.GREEN + areas.size() + ChatColor.GOLD + "): ");
                for(int i = 0; i < areas.size(); i++) {
                    BridgingArea area = areas.get(i);
                    ChatColor color;
                    TextComponent text = new TextComponent((color = (area.isClosed() ? ChatColor.GRAY : (area.isAvailable() ? ChatColor.GREEN : ChatColor.RED)))
                            + area.getAreaName() + (i < areas.size() - 1 ? ", " : ""));
                    text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(color + (area.isClosed() ? "Closed" : (area.isAvailable() ? "Available" : "Occupied (by " + area.getPlayer().getName() + ")"))).create()));
                    msg.addExtra(text);
                }
                p.spigot().sendMessage(msg);
                break;
            case "create":
                if(args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging create <name>");
                }else {
                    String name = args[1];
                    if(manager.getArea(name) != null) {
                        p.sendMessage(ChatColor.RED + "An area with this name already exists!");
                    }else {
                        Selection s = getWorldEdit().getSelection(p);
                        if(s == null) throw new NullPointerException("You need to have a worldedit selection as the bridging area!");
                        ProtectedCuboidRegion region = new ProtectedCuboidRegion(name + "_area_" + randomLetters(),
                                fromLocation(s.getMinimumPoint()),
                                fromLocation(s.getMaximumPoint()));
                        manager.setAreaFlags(region);
                        if(s.getWorld() == null) throw new NullPointerException("World doesn't exist?");
                        RegionManager rm = WorldGuardPlugin.inst().getRegionContainer().get(s.getWorld());
                        if(rm == null) throw new NullPointerException("WorldGuard of " + s.getWorld().getName() + " doesn't exist? Did you load the world, yet?");
                        rm.addRegion(region);
                        ConfigurationSection section = manager.config.createSection("areas." + name);
                        section.set("name", name);
                        section.set("world", s.getWorld().getName());
                        section.set("worldguard", region.getId());
                        manager.updated = true;
                        p.sendMessage(
                                String.format("%sSuccessfully created area with name %s%s%s in world %s as location from %s to %s",
                                        ChatColor.GREEN,
                                        ChatColor.GOLD,
                                        args[1],
                                        ChatColor.GREEN,
                                        s.getWorld().getName(),
                                        xyzFromLocation(s.getMinimumPoint()),
                                        xyzFromLocation(s.getMaximumPoint())));
                        p.sendMessage(ChatColor.GOLD + "Note: You still need to set the finish line and spawnpoint of the area for the area to be loaded.");
                    }
                }
                break;
            case "delete":
                if(args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging delete <area>");
                }else {
                    ConfigurationSection section = manager.config.getConfigurationSection("areas." + args[1]);
                    if(section == null) throw new NullPointerException("Area not found! (in config)");
                    String worldName = section.getString("world");
                    if(worldName != null) {
                        World world = Bukkit.getWorld(worldName);
                        if(world != null) {
                            RegionManager rm = WorldGuardPlugin.inst().getRegionManager(world);
                            rm.removeRegion(section.getString("finish_line","IgnoreThis:))))"));
                            rm.removeRegion(section.getString("worldguard","IgnoreThis:))))"));
                        }
                    }
                    manager.config.set("areas." + args[1], null);
                    manager.updated = true;
                    p.sendMessage(ChatColor.GREEN + "Area successfully deleted! You might need to do /bridging reload.");
                }
                break;
            case "setspawn":
                if(args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging setspawn <area>");
                }else {
                    ConfigurationSection config = manager.config.getConfigurationSection("areas." + args[1]);
                    if(config == null) throw new NullPointerException("Area not found!");
                    Location loc = p.getLocation();
                    ConfigurationSection section = config.isConfigurationSection("spawnpoint") ? config.getConfigurationSection("spawnpoint") : config.createSection("spawnpoint");
                    section.set("x", loc.getX());
                    section.set("y", loc.getY());
                    section.set("z", loc.getZ());
                    section.set("yaw", (double) loc.getYaw());
                    section.set("pitch", (double) loc.getPitch());
                    manager.updated = true;
                    p.sendMessage(ChatColor.GREEN + "Successfully set the spawn of area " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " with your current location: " + xyzFromLocation(loc));
                }
                break;
            case "setarea":
                if(args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging setarea <area>");
                }else {
                    Selection s = getWorldEdit().getSelection(p);
                    ConfigurationSection config = manager.config.getConfigurationSection("areas." + args[1]);
                    if(config == null) throw new NullPointerException("Area not found!");
                    String worldName;
                    if(s == null || s.getWorld() == null) throw new NullPointerException("You either do not have a selection or have an invalid one.");
                    if(!(worldName = config.getString("world")).equals(s.getWorld().getName())) throw new IllegalStateException("Wrong world! The area is supposed to be in world '" + worldName + "'");
                    RegionManager rm = WorldGuardPlugin.inst().getRegionManager(s.getWorld());
                    String regionName = config.getString("worldguard");
                    if(regionName != null) {
                        ProtectedRegion previousArea = rm.getRegion(regionName);
                        if(previousArea != null) rm.removeRegion(previousArea.getId());
                    }
                    Location min = s.getMinimumPoint();
                    Location max = s.getMaximumPoint();

                    ProtectedCuboidRegion area = new ProtectedCuboidRegion(args[1] + "_area_" + randomLetters(), fromLocation(min), fromLocation(max));
                    manager.setAreaFlags(area);
                    rm.addRegion(area);
                    config.set("worldguard", area.getId());
                    manager.updated = true;
                    p.sendMessage(ChatColor.GREEN + "Successfully set the area of " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " with the current worldedit selection from " + xyzFromLocation(min) + " to " + xyzFromLocation(max));
                }
                break;
            case "setfinish":
                if(args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging setfinish <area>");
                }else {
                    Selection s = getWorldEdit().getSelection(p);
                    ConfigurationSection config = manager.config.getConfigurationSection("areas." + args[1]);
                    if(config == null) throw new NullPointerException("Area not found!");
                    String worldName;
                    if(s == null || s.getWorld() == null) throw new NullPointerException("You either do not have a selection or have an invalid one.");
                    if(!(worldName = config.getString("world")).equals(s.getWorld().getName())) throw new IllegalStateException("Wrong world! The finish line is supposed to be in world '" + worldName + "'");
                    RegionManager rm = WorldGuardPlugin.inst().getRegionManager(s.getWorld());
                    String regionName = config.getString("worldguard");
                    if(regionName == null) throw new NullPointerException("The worldguard of the area does not exist. It must be set before this is!");
                    ProtectedRegion region = rm.getRegion(regionName);
                    Location min = s.getMinimumPoint();
                    Location max = s.getMaximumPoint();
                    if(region == null) throw new NullPointerException("Area in WorldGuard not found!");
                    if(!(region.contains(min.getBlockX(), min.getBlockY(), min.getBlockZ()) &&
                            region.contains(max.getBlockX(), max.getBlockY(), max.getBlockZ()))) throw new IllegalStateException("The finish line MUST be inside the worldguard of the area");

                    String previousFinish;
                    if((previousFinish = config.getString("finish_line")) != null) {
                        rm.removeRegion(previousFinish);
                    }

                    ProtectedCuboidRegion finish = new ProtectedCuboidRegion(args[1] + "_finish_" + randomLetters(), fromLocation(min), fromLocation(max));
                    manager.setAreaFlags(finish);
                    rm.addRegion(finish);
                    config.set("finish_line", finish.getId());
                    manager.updated = true;
                    p.sendMessage(ChatColor.GREEN + "Successfully set the finish line of " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " with the current worldedit selection from " + xyzFromLocation(min) + " to " + xyzFromLocation(max));
                }
                break;
            case "setlobby":
                if(true) {
                    Location loc = p.getLocation();
                    Configuration config = manager.config;
                    ConfigurationSection lobby = config.getConfigurationSection("lobby");
                    if(lobby == null) lobby = config.createSection("lobby");
                    lobby.set("world", loc.getWorld().getName());
                    lobby.set("x", loc.getX());
                    lobby.set("y", loc.getY());
                    lobby.set("z", loc.getZ());
                    lobby.set("yaw", (double) loc.getYaw()); // I don't think floats are supported in YAMLConfiguration (only FloatList)
                    lobby.set("pitch", (double) loc.getPitch());
                    manager.updated = true;
                    p.sendMessage(ChatColor.GREEN + "Successfully set the lobby to your current location.");
                }
                break;
            case "blocknpc":
                if(Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
                    NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "Miner");
                    npc.addTrait(BlockSelectorTrait.class);
                    npc.spawn(p.getLocation(), SpawnReason.COMMAND);
                    npc.setName(ChatColor.GOLD + "Block Selector");
                    npc.data().set("player-skin-name", "Miner");
                    p.sendMessage(ChatColor.GREEN + "Successfully created the NPC! You can modify it by doing /npc");
                }else p.sendMessage(ChatColor.RED + "Citizens is not installed in this server!");
                break;
            case "clearstat":
                if(args.length < 3) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging clearstat <player> [stat|all]\n" +
                            "Stats available:\n" +
                            "average_time, personal_best, blocks_placed, coins, bridge_count, failed_attempts, owned_blocks");
                }else {
                    Map.Entry<BridgingStats, String> output = manager.getStats(args[1]);
                    BridgingStats stats = output.getKey();
                    String name = output.getValue();
                    if(stats == null) throw new NullPointerException("Player not found!");
                    Object value;
                    if(args[2].equalsIgnoreCase("all")) {
                        value = stats;
                        stats.resetAndSave();
                    }else if(args[2].equalsIgnoreCase("average_time"))
                        value = stats.resetAverageTime();
                    else if(args[2].equalsIgnoreCase("personal_best"))
                        value = stats.setPersonalBest(0.0d);
                    else if(args[2].equalsIgnoreCase("blocks_placed"))
                        value = stats.resetPlacedBlocks();
                    else if(args[2].equalsIgnoreCase("coins"))
                        value = stats.resetCoins();
                    else if(args[2].equalsIgnoreCase("bridge_count"))
                        value = stats.setBridgeCount(0);
                    else if(args[2].equalsIgnoreCase("failed_attempts"))
                        value = stats.setFailedAttempts(0);
                    else if(args[2].equalsIgnoreCase("owned_blocks")) {
                        value = new ArrayList<>(stats.getOwnedBlocks());
                        stats.getOwnedBlocks().clear();
                        manager.updated = true;
                    }
                    else throw new NullPointerException("Invalid stat!");
                    p.sendMessage(ChatColor.GREEN + "Successfully cleared " + name + "'s statistic of " + args[2] + " (previously " + value + ")");
                }
                break;
            case "clearstats":
                if(args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging clearstats <player>");
                }else {
                    String name = args[1];
                    UUID uuid;
                    if(BridgingStats.UUID_CHECK.matcher(args[1]).matches()) {
                        UUID uuid2 = UUID.fromString(args[1]);
                        Player player = Bukkit.getPlayer(uuid2);
                        if(player == null) {
                            OfflinePlayer off = Bukkit.getPlayer(uuid2);
                            if(off == null) throw new NullPointerException("Player not found!");
                            name = off.getName();
                        }else name = player.getName();
                        uuid = uuid2;
                    }else {
                        Player player = Bukkit.getPlayer(name);
                        if(player == null) {
                            OfflinePlayer off = Bukkit.getPlayer(name);
                            if(off == null) throw new NullPointerException("Player not found!");
                            uuid = off.getUniqueId();
                        }else uuid = player.getUniqueId();
                    }
                    if(uuid == null) throw new NullPointerException("Player not found!");
                    BridgingStats stats = manager.getStatsWithoutDefault(uuid);
                    if(stats == null) throw new NullPointerException("Player does not have any statistics!");
                    stats.resetAndSave();
                    p.sendMessage(ChatColor.GREEN + "Successfully resetted " + name + "'s statistics!");
                }
                break;
            case "getlb":
                if(args.length < 3) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging getlb <coins|average|pb> <number> [number_to]\n" +
                            "'coins' for most coins. 'average' for lowest average time. 'pb' for lowest personal best (time).\n" +
                            "number_to example:\n" +
                            "Without: Only the number\n" +
                            "With: From the number UNTIL [number_to] (like #1-50 lb)");
                }else {
                    int from, to;
                    from = to = Integer.parseInt(args[2]);
                    if(args.length > 3) to = Integer.parseInt(args[3]);
                    if(from < 1 || to < 1) throw new NumberFormatException("Number must be 1 or above");
                    if(to < from) throw new NumberFormatException("[number_to] must be bigger than original number.");
                    if(to - from + 1 > 50) throw new IllegalArgumentException("Only allowed max. 50 lb/command");
                    List<UUID> topList = args[1].equalsIgnoreCase("coins") ?
                            BridgingStats.getTopCoins() : (
                            args[1].equalsIgnoreCase("average") ?
                                    BridgingStats.getTopAverageTime() :
                                    (args[1].equalsIgnoreCase("pb") ?
                                            BridgingStats.getTopPB() :
                                            null)
                    );
                    if(topList == null) throw new IllegalArgumentException("Leaderboard type must either be: coins, average, or pb");
                    String endWithS = args[1].equalsIgnoreCase("coins") ? "" : "s";
                    ChatColor color = endWithS.isEmpty() ? ChatColor.YELLOW : (args[1].equalsIgnoreCase("average") ? ChatColor.AQUA : ChatColor.GOLD);
                    String str = "";
                    for(int i = from - 1; i < to; i++) {
                        if(topList.size() >= i + 1) {
                            UUID uuid = topList.get(i);
                            String name = uuid.toString();
                            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                            if(player != null) name = player.getName();
                            BridgingStats stats = manager.getStats(uuid);
                            String value = "" + (endWithS.isEmpty() ? stats.getCoins() :
                                    (color == ChatColor.AQUA ? stats.getAverageTime() : stats.getPersonalBest()));
                            str += ChatColor.DARK_PURPLE + "#" + (i + 1) + ". " + name + " [" + color + value + endWithS + ChatColor.DARK_PURPLE + "]\n";
                        }else {
                            if(from == to) str += ChatColor.GRAY + "#" + from + ". No one!";
                            else str += ChatColor.GRAY + "#" + (i + 1) + "-" + to + ". No one!\n";
                            break;
                        }
                    }
                    p.sendMessage(ChatColor.LIGHT_PURPLE + "Leaderboard of " + args[1] + " from #" + from + " to #" + to + ":\n" +
                            str);
                }
                break;
            case "reload":
                manager.config.load(manager.configFile);
                p.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
                manager.getAreas().forEach(area -> {
                    if(area.getPlayer() != null) {
                        Player player = area.getPlayer();
                        if(player.isOnline()) {
                            area.leave();
                            player.sendMessage(ChatColor.RED + "Bedwars Bridging has been reloaded. You have been kicked from your Bridging Area.");
                        }
                    }
                    HandlerList.unregisterAll(area); // Remove them as a Listener to events
                    Bukkit.getScheduler().cancelTask(area.repeatingTaskId);
                });
                p.sendMessage(ChatColor.RED + "Force-left all players in a bridging area");
                manager.hideScoreboardAll();
                p.sendMessage(ChatColor.RED + "Hid scoreboard for all players (in a bridging area)");
                manager.getAreas().clear();
                p.sendMessage(ChatColor.YELLOW + "Cleared all areas currently loaded!");
                manager.load();
                p.sendMessage(ChatColor.GREEN + "Successfully loaded Bedwars Bridging!");
                break;
            case "setstat":
                if(args.length < 4) {
                    p.sendMessage(ChatColor.RED + "Usage: /bridging setstat <player> <stat> <value>\n" +
                            "All available stat:\n" +
                            "average_time, personal_best, blocks_placed, coins, bridge_count, failed_attempts");
                }else {
                    Map.Entry<BridgingStats, String> entry = manager.getStats(args[1]);
                    BridgingStats stats = entry.getKey();
                    String name = entry.getValue();
                    if(stats != null && name != null) {
                        String statName = args[2];
                        boolean a;
                        // Double
                        if((a = statName.equalsIgnoreCase("average_time")) || statName.equalsIgnoreCase("personal_best")) {
                            Double value = getDouble(args[3]);
                            if(value == null) throw new NumberFormatException("Invalid value. Stat type is double (a number with/out decimals). For type value: \"" + args[2] + "\"");
                            double old = a ? stats.getAverageTime() : stats.getPersonalBest();
                            if(a) stats.setAverageTime(value);
                            else stats.setPersonalBest(value);
                            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&aSuccessfully set &6" + name + "&a's stat of " +
                                            "&6" + statName + "&a to &6" + value + "&2s (previously &6" + old + "&2s)"));
                        }else {
                            BiConsumer<BridgingStats, Integer> set;
                            Function<BridgingStats, Integer> get;
                            if(statName.equalsIgnoreCase("blocks_placed")) {
                               set = BridgingStats::setPlacedBlocks;
                               get = BridgingStats::getBlocksPlaced;
                            }else if(statName.equalsIgnoreCase("coins")) {
                                set = BridgingStats::setCoins;
                                get = BridgingStats::getCoins;
                            }else if(statName.equalsIgnoreCase("bridge_count")) {
                                set = BridgingStats::setBridgeCount;
                                get = BridgingStats::getBridgeCount;
                            }else if(statName.equalsIgnoreCase("failed_attempts")) {
                                set = BridgingStats::setFailedAttempts;
                                get = BridgingStats::getFailedAttempts;
                            }else throw new IllegalArgumentException("Invalid stat name. Available stat names: average_time, personal_best, blocks_placed, coins, bridge_count, failed_attempts");
                            int value;
                            try {
                                value = Integer.parseInt(args[3]);
                            }catch(Exception e1) {
                                throw new NumberFormatException("Invalid value. Stat type is integer (a number without decimals). For type value: \"" + args[3] + "\"");
                            }
                            int old = get.apply(stats);
                            set.accept(stats, value);
                            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&aSuccessfully set &6" + name + "&a's stat of &6" +
                                            statName + "&a to &6" + value + "&a(previously &6" + old + "&a)"));
                        }
                    }
                }
                break;
            case "setgroup":
                if(args.length < 3) {
                    p.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /bridging setgroup <area> <group>");
                }else {
                    String areaName = args[1];
                    String group = args[2];
                    BridgingArea area = manager.getArea(areaName);
                    if(area == null) {
                        p.sendMessage(ChatColor.RED + "Area not found.");
                    }else {
                        if(area.getAreaGroup().equals(group)) {
                            p.sendMessage(ChatColor.RED + "Area is already in that group.");
                        }else {
                            boolean has = false;
                            for(String group2 : manager.getAreaGroups().keySet()) {
                                if(group2.equals(group)) {
                                    has = true; break;
                                }
                            }
                            if(!has) {
                                p.sendMessage(ChatColor.GOLD + "Group not found. Creating it...");
                                ArrayList<String> list = new ArrayList<>();
                                list.add(areaName);
                                manager.getAreaGroups().put(group, list);
                            }else {
                                manager.getAreaGroups().get(group).add(areaName);
                            }

                            area.setAreaGroup(group);

                            manager.updated = true;
                            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&aSuccessfully set &6" + areaName + "&a's group to &6" + group));
                        }
                    }
                }
        }

    }

    @Override
    public boolean allowsConsole() {
        return true;
    }

    @Override
    public @Nullable String getArgPermission(int index) {
        return "bedwarspractice.bridging." + validArguments().get(index);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String mainArg, String[] args) {
        if(sender instanceof Player && !sender.hasPermission("bedwarspractice.bridging." + mainArg)) return null;
        if(args.length == 0) return null;
        switch(mainArg.toLowerCase()) {
            case "join":
            case "kick":
            case "close":
            case "open":
            case "delete":
            case "setspawn":
            case "setarea":
            case "setfinish":
                if(args.length > 1) break;
                return manager.getAreas().stream().map(BridgingArea::getAreaName).collect(Collectors.toList());
            case "stats":
            case "forceplay":
                if(args.length > 1) break;
                if(mainArg.equalsIgnoreCase("stats") && sender instanceof Player && !sender.hasPermission("bedwarspractice.bridging.stats.other")) break;
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            case "forcejoin":
                if(args.length > 2) break;
                if(args.length == 1)
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());

                return manager.getAreas().stream().map(BridgingArea::getAreaName).collect(Collectors.toList());
            case "clearstat":
                if(args.length > 2) break;
                if(args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                return Arrays.asList("all", "average_time", "personal_best", "blocks_placed", "coins", "bridge_count", "failed_attempts", "owned_blocks");
            case "clearstats":
                if(args.length > 1) break;
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            case "getlb":
                if(args.length > 3) break;
                if(args.length == 1) return Arrays.asList("coins", "average", "pb");
                if(args.length == 2)
                    return Arrays.asList(ChatColor.GRAY + "Enter a number", "1", "2", "3", "50");
                return
                        Arrays.asList(ChatColor.GRAY + "Enter a number [to] (previous number - this number) e.g 1 - 50", "2", "3", "4", "100");
            case "setstat":
                if(args.length > 3) break;
                if(args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                else if(args.length == 2) return Arrays.asList("average_time", "personal_best", "blocks_placed", "coins", "bridge_count", "failed_attempts");
                if(args[2].equalsIgnoreCase("average_time") || args[2].equalsIgnoreCase("personal_best"))
                    return Arrays.asList(ChatColor.GRAY + "Enter a double (a number with/without decimals)", "1", "3", "4", "2.33", "9.11129", "69.420");
                else if(args[2].equalsIgnoreCase("blocks_placed") ||
                args[2].equalsIgnoreCase("coins") ||
                args[2].equalsIgnoreCase("bridge_count") ||
                        args[2].equalsIgnoreCase("failed_attempts"))
                    return Arrays.asList(ChatColor.GRAY + "Enter an integer (a number without decimals)", "1", "2", "3", "50", "123", "20000", "123456789");
                return Arrays.asList(ChatColor.RED + "The stat you picked is invalid, and therefore has no type", ChatColor.GRAY + "Valid stat types: average_time, personal_best, blocks_placed, coins, bridge_count, failed_attempts");
            default:
                break;
        }
        return null;
    }

    public BridgingCommand(BedwarsPractice main) {
        this.main = main;
        manager = main.getBridgingManager();
    }
    public WorldEditPlugin getWorldEdit() {
        Plugin pl = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if(pl instanceof WorldEditPlugin) return (WorldEditPlugin) pl;
        main.getLogger().severe("The WorldEdit version currently installed is NOT recognized by the plugin. Although, this will only cause errors upon creating a bridging area.");
        return null;
    }
    private String randomLetters() {
        return UUID.randomUUID().toString().substring(0, 2);
    }
    private BlockVector fromLocation(Location loc) {
        return BlockVector.toBlockPoint(loc.getX(), loc.getY(), loc.getZ());
    }
    private String xyzFromLocation(Location loc) {
        return "[" + floor(loc.getX()) + ", " + floor(loc.getY()) + ", " + floor(loc.getZ()) + "]";
    }
    private int floor(double d) {
        return (int) Math.floor(d);
    }
    private @Nullable Double getDouble(String str) {
        try {
            return Double.parseDouble(str);
        }catch(Exception e1) {
            try {
                return (double) Integer.parseInt(str);
            }catch(Exception ignored) {}
        }
        return null;
    }
    private List<String> alternatingColors(List<String> list, ChatColor... colors) {
        List<String> newList = new ArrayList<>();
        for(int current = 0; current < list.size(); current++) {

            String str = list.get(current);
            String replaced;

            if(!(replaced = str.replace(" -s", "")).equals(str) ||
                    !(replaced = str.replace("-s", "")).equals(str)) {
                newList.add(replaced);
                continue;
            }
            ChatColor color = colors[current % colors.length];
            newList.add(color + str);
        }
        return newList;
    }
    private String adminHelp;
    private String getAdminHelp() {
        if(adminHelp != null) return adminHelp;
        StringBuilder builder = new StringBuilder();
        List<String> list = alternatingColors(Arrays.asList(
                "\n" + ChatColor.GOLD + "Commands for areas: -s",
                "/bridging kick <player>: Forces that player to leave",
                "/bridging forceplay <player>: Forces that player to join any available area",
                "/bridging forcejoin <player> <area>: Forces that player to join the area (if available)",
                "/bridging close <area>: Closes that area so it's permanently not available",
                "/bridging open <area>: Opens that area if it's not available",
                "/bridging areas: Returns a list of areas",
                "\n" + ChatColor.GOLD + "Commands for modifying with areas: -s",
                "/bridging create <area>: Creates a bridging area with the selected worldedit selection",
                "/bridging delete <area>: Deletes the area from config.",
                "/bridging setspawn <area>: Sets the spawn for the area in your location",
                "/bridging setarea <area>: Re-set the bridging area.",
                "/bridging setfinish <area>: Sets the finish line for the are with the selected worldedit selection",
                "/bridging setlobby: Sets the lobby position for your current location (including yaw and pitch)\n",
                "/bridging setgroup <area> <group>: Sets the group for the area. Used for specific PB (specifically 16/32/64 blocks)",
                "/bridging blocknpc: [Citizens only] Creates an NPC for selecting blocks (Only if citizens is installed)",
                "\n" + ChatColor.GOLD + "\nCommands for player statistics: -s",
                "/bridging stats <player>: Gets that player's statistics",
                "/bridging clearstats <player>: Clears that player's statistics",
                "/bridging getlb <coins|average|pb> <number> [number_to]: Type without any args for help",
                "/bridging clearstat <player> <stat|all>: Clears that player's stat. All available stat will be shown when using the command.",
                "/bridging setstat <player> <stat> <value>: Sets that player's stat with the value. Error if type is incompatible with input\n" +
                        "Slight note: Modifying stats does not need reload",

                "/bridging reload: Reloads the config for bridging.yml"
        ), ChatColor.AQUA, ChatColor.DARK_AQUA);
        list.forEach((str) -> builder.append(str).append('\n'));
        adminHelp = builder.substring(0, builder.length() - 2);
        return adminHelp;
    }
}
