package me.bramar.bedwarspractice.worldguard;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * When a player exits one/multiple region(s)
 */
public class PlayerExitRegionEvent extends PlayerEvent implements Cancellable {
    private boolean cancelled = false;
    private static final HandlerList handlers = new HandlerList();
    private final Collection<ProtectedRegion> regions;

    public PlayerExitRegionEvent(Player player, Collection<ProtectedRegion> regions) {
        super(player);
        this.regions = regions;
    }

    public Collection<ProtectedRegion> getRegions() {
        return regions;
    }
    public boolean hasRegion(String regionName) {
        for(ProtectedRegion region : regions) {
            if(regionName.equalsIgnoreCase(region.getId())) return true;
        }
        return false;
    }
    public @Nullable ProtectedRegion getExitedRegion(String regionName) {
        for(ProtectedRegion region : regions) {
            if(regionName.equalsIgnoreCase(region.getId())) return region;
        }
        return null;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
