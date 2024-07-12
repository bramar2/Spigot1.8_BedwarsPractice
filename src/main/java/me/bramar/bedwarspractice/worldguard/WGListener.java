package me.bramar.bedwarspractice.worldguard;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;


public class WGListener implements Listener {
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        // From WorldGuard to check if player has indeed moved 1 block (at least)
        if (from.getBlockX() != to.getBlockX() ||
                from.getBlockY() != to.getBlockY() ||
                from.getBlockZ() != to.getBlockZ()) {
            Set<ProtectedRegion> exit = new HashSet<>();
            Set<ProtectedRegion> enter = new HashSet<>();
            RegionManager rm = WorldGuardPlugin.inst().getRegionManager(e.getPlayer().getWorld());
            for (ProtectedRegion region : rm.getRegions().values()) {
                boolean boolFrom, boolTo;
                boolFrom = boolTo = false;
                if(region.contains(fromLocation(from))) boolFrom = true;
                if(region.contains(fromLocation(to))) boolTo = true;
                if(boolFrom && !boolTo) exit.add(region);
                else if(boolTo && !boolFrom) enter.add(region);
            }
            if (!enter.isEmpty()) {
                PlayerEnterRegionEvent e1 = new PlayerEnterRegionEvent(e.getPlayer(), enter);
                Bukkit.getPluginManager().callEvent(e1);
                if (e1.isCancelled()) {
                    e.setCancelled(true);
                    return;
                }
            }
            if (!exit.isEmpty()) {
                PlayerExitRegionEvent e1 = new PlayerExitRegionEvent(e.getPlayer(), exit);
                Bukkit.getPluginManager().callEvent(e1);
                if (e1.isCancelled()) e.setCancelled(true);
            }
        }
    }
    private BlockVector fromLocation(Location loc) {
        return BlockVector.toBlockPoint(loc.getX(), loc.getY(), loc.getZ());
    }
}
