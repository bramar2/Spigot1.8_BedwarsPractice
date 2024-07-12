package me.bramar.bedwarspractice.placeholders;

import me.bramar.bedwarspractice.BedwarsPractice;
import me.bramar.bedwarspractice.bridging.BridgingStats;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class CPSPlaceholders implements PlaceholderListener, Listener {
    private final Map<UUID, List<Long>> leftCPS = new HashMap<>();
    private final Map<UUID, List<Long>> rightCPS = new HashMap<>();
    private final BedwarsPractice main;
    public CPSPlaceholders() {
        main = JavaPlugin.getPlugin(BedwarsPractice.class);
        Bukkit.getPluginManager().registerEvents(this, main);
    }
    public int getLeftCPS(UUID uuid) {
        if(leftCPS.containsKey(uuid)) {
            List<Long> old = leftCPS.get(uuid);
            if(old.isEmpty()) return 0;
            List<Long> list = old
                    .stream()
                    .filter(tick -> main.getCurrentTick() - tick <= 20)
                    .collect(Collectors.toList());
            if(old.size() != list.size()) leftCPS.put(uuid, list);
            return list.size();
        }else leftCPS.put(uuid, new ArrayList<>());
        return 0;
    }
    public int getRightCPS(UUID uuid) {
        if(rightCPS.containsKey(uuid)) {
            List<Long> old = rightCPS.get(uuid);
            if(old.isEmpty()) return 0;
            List<Long> list = old
                    .stream()
                    .filter(tick -> main.getCurrentTick() - tick <= 20)
                    .collect(Collectors.toList());
            if(old.size() != list.size()) rightCPS.put(uuid, list);
            return list.size();
        }else rightCPS.put(uuid, new ArrayList<>());
        return 0;
    }

    public void addLeftCPS(UUID uuid) {
        List<Long> list = leftCPS.getOrDefault(uuid, new ArrayList<>());
        list.add(main.getCurrentTick());
        leftCPS.put(uuid, list);
    }

    public void addRightCPS(UUID uuid) {
        List<Long> list = rightCPS.getOrDefault(uuid, new ArrayList<>());
        list.add(main.getCurrentTick());
        rightCPS.put(uuid, list);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if(e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) addLeftCPS(uuid);
        else if(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) addRightCPS(uuid);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        leftCPS.remove(e.getPlayer().getUniqueId());
        rightCPS.remove(e.getPlayer().getUniqueId());
    }
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params0) {
        String[] params = params0.split("_");
        if(params[0].equalsIgnoreCase("cps")) {
            boolean left = params[1].equalsIgnoreCase("left");
            if(!(left || params[1].equalsIgnoreCase("right"))) return null; // Neither left or right
            if(params.length > 2) {
                Player p = BridgingStats.UUID_CHECK.matcher(params[2]).matches() ?
                        Bukkit.getPlayer(UUID.fromString(params[2])) :
                        Bukkit.getPlayer(params[2]);
                if(p.isOnline()) return String.valueOf((left) ? getLeftCPS(p.getUniqueId()) : getRightCPS(p.getUniqueId()));
            }else if(player.isOnline()) {
                return String.valueOf((left) ? getLeftCPS(player.getUniqueId()) : getRightCPS(player.getUniqueId()));
            }
        }
        return null;
    }
}
