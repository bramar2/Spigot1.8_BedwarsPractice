package me.bramar.bedwarspractice;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import me.bramar.bedwarspractice.blockin.BlockinCommand;
import me.bramar.bedwarspractice.blockin.BlockinManager;
import me.bramar.bedwarspractice.bridging.BridgingCommand;
import me.bramar.bedwarspractice.bridging.BridgingManager;
import me.bramar.bedwarspractice.placeholders.BedwarsPlaceholders;
import me.bramar.bedwarspractice.placeholders.CPSPlaceholders;
import me.bramar.bedwarspractice.utils.pagedinv.PagedInventoryListener;
import me.bramar.bedwarspractice.worldguard.WGListener;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class BedwarsPractice extends JavaPlugin {

    public static void main(String[] args) {
        System.out.println(Arrays.asList("kick", "forceplay", "forcejoin", "close", "open", "areas",
                "create", "delete", "setspawn", "setarea", "setfinish", "setlobby",
                "blocknpc", "stats", "clearstat", "clearstats", "getlb", "reload"));
    }

    private long currentTick = 0;
    private boolean intentionalDisable = false;
    private BridgingManager bridging;
    private BlockinManager blockin;
    private BedwarsPlaceholders placeholders;
    private ProtocolManager protocolManager;
    @Override
    public void onEnable() {
        if(!(getServer().getPluginManager().isPluginEnabled("WorldEdit") && getServer().getPluginManager().isPluginEnabled("WorldGuard"))) {
            intentionalDisable = true;
            getLogger().severe("WorldEdit and WorldGuard needs to be installed for this plugin to work!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new WGListener(), this);
        getServer().getPluginManager().registerEvents(new PagedInventoryListener(), this);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> currentTick++, 1, 1);
        getLogger().info("Loaded event listeners");

        bridging = new BridgingManager();
        getLogger().info("Loaded Bridging Manager");

//        blockin = new BlockinManager();
//        getLogger().info("Loaded Blockin Manager");

        new BridgingCommand(this).register();
        new BlockinCommand(this).register();
        getLogger().info("Loaded commands");

        File schemFolder = new File(getDataFolder(), "schematic");
        if(!schemFolder.exists()) if(!schemFolder.mkdirs()) {
            getLogger().warning("Failed to create schematic folder!");
        }

        List<String> softDepends = getDescription().getSoftDepend();
        if(softDepends != null) {
            List<String> list = softDepends.stream().filter(Bukkit.getPluginManager()::isPluginEnabled).collect(Collectors.toList());
            if(list.contains("ProtocolLib")) protocolManager = ProtocolLibrary.getProtocolManager();
            getLogger().info("Soft-dependencies loaded: " + list);
            List<String> unloaded = new ArrayList<>(softDepends);
            unloaded.removeAll(list);
            if(!unloaded.isEmpty()) getLogger().info("Soft-dependencies not loaded: " +
                    unloaded +
                    ". To enjoy the best experience, install all soft dependencies to maximize the quality of this plugin."
            );
        }

        placeholders = new BedwarsPlaceholders();
        placeholders.register();
        placeholders.register(new CPSPlaceholders());
        getLogger().info("Placeholders loaded!");

        getLogger().info("BedwarsPractice have been enabled!");
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public BridgingManager getBridgingManager() {
        return bridging;
    }

    @Override
    public void onDisable() {
        if(!intentionalDisable) getLogger().info("BedwarsPractice have been disabled!");
        bridging.getAreas().forEach(area -> {
            if(area.getPlayer() != null && !area.isAvailable()) area.leave();
        });
        bridging.saveConfigNow();
    }

    @Override
    public void saveResource(String resourcePath, boolean replace) {
        Validate.isTrue(!(resourcePath == null || resourcePath.isEmpty()), "ResourcePath cannot be null or empty");

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        Validate.notNull(in, "The embedded resource '" + resourcePath + "' cannot be found in " + getFile());
        File outFile = new File(getDataFolder(), resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(getDataFolder(), resourcePath.substring(0, Math.max(lastIndex, 0)));

        if (!outDir.exists()) if (!outDir.mkdirs())
            throw new RuntimeException("Folder of " + getDataFolder().getPath() + " cannot be created for a file to be saved inside it");

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
        }
    }

    public BedwarsPlaceholders getPlaceholders() {
        return placeholders;
    }

    public void startXPCooldown(Player p, int ticksP, String endMessage, Runnable run) {
        if(ticksP > 0) new BukkitRunnable() {
            int ticks = ticksP;
            final int startLvl = p.getLevel();
            final float startXP = p.getExp();
            @Override
            public void run() {
                // If quitted
                if(!p.isOnline()) {
                    cancel();
                    return;
                }
                if(ticks <= 0) {
                    p.sendTitle(ChatColor.translateAlternateColorCodes('&', endMessage), null);
                    p.playSound(p.getLocation(), Sound.LEVEL_UP, 1, 1);
                    p.setLevel(startLvl);
                    p.setExp(startXP);
                    cancel();
                    if(run != null) run.run();
                    return;
                }
                // Every second
                if(ticks % 20 == 0) {
                    String msg = ChatColor.GOLD.toString() + (ticks / 20);
                    p.sendTitle(msg, null);
                    p.playSound(p.getLocation(), Sound.LEVEL_UP, 1, 1);
                }
                p.setLevel((int) Math.floor(ticks / 20d));
                float xp = (ticks - (ticks % 20)) / 20f;
                xp = xp >= 0.95f ? 0.949f : (xp <= 0.05f ? 0.051f : xp);
                p.setExp(xp);
                ticks--;
            }
        }.runTaskTimer(this, 1, 0);
    }

    public BlockinManager getBlockinManager() {
        return blockin;
    }
}
