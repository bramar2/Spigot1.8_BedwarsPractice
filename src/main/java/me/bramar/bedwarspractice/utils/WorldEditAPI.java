package me.bramar.bedwarspractice.utils;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.World;
import me.bramar.bedwarspractice.BedwarsPractice;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunkBulk;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class WorldEditAPI {
    private static final WorldEdit we;
    private static final BedwarsPractice main;
    private static final WorldEditPlugin wep;
    static {
        we = WorldEdit.getInstance();
        wep = JavaPlugin.getPlugin(WorldEditPlugin.class);
        main = JavaPlugin.getPlugin(BedwarsPractice.class);
    }
    private WorldEditAPI() {}

    public static void save(final Player p, final String schematicName) {
        try {
            File schematic = new File(main.getDataFolder(), schematicName);
            File parentFile = schematic.getParentFile();
            if(!parentFile.exists()) if(!parentFile.mkdirs()) throw new IOException("Failed to create directory " + parentFile.getPath());

            com.sk89q.worldedit.entity.Player actor = wep.wrapPlayer(p);
            LocalSession session = we.getSessionManager().getIfPresent(actor);
            if(session == null) throw new IllegalStateException("Player does not have have a clipboard/something copied!");
            ClipboardHolder selection = session.getClipboard();
            EditSession editSession = session.createEditSession(actor);
            Vector min = selection.getClipboard().getMinimumPoint();
            Vector max = selection.getClipboard().getMaximumPoint();

            editSession.enableQueue();
            CuboidClipboard clipboard = new CuboidClipboard(max.subtract(min).add(new Vector(1, 1, 1)), min);
            clipboard.rotate2D(180);
            clipboard.copy(editSession);
            SchematicFormat.MCEDIT.save(clipboard, schematic);
            editSession.flushQueue();
        }catch(IOException | DataException | EmptyClipboardException e) {
            main.getLogger().log(Level.SEVERE, "[WorldEditAPI] Failed to save a schematic to file " + schematicName + "!", e);
            throw new RuntimeException("An internal error occurred! Check console for stack trace.", e);
        }
    }
    public static void paste(CuboidClipboard clipboard, Location loc) {
        try {
            World weWorld = new BukkitWorld(loc.getWorld());
            EditSession editSession = we.getEditSessionFactory().getEditSession(weWorld, -1);
            editSession.enableQueue();
            clipboard.paste(editSession, BukkitUtil.toVector(loc), false);
            editSession.flushQueue();
        }catch(MaxChangedBlocksException e) {
            main.getLogger().log(Level.SEVERE, "[WorldEditAPI] Failed to paste a schematic into world " + loc.getWorld() + "!", e);
            throw new RuntimeException("An internal error occurred! Check console for stack trace.", e);
        }
    }
    public static CuboidClipboard getSchematic(String schematicName) {
        try {
            File schematicFile = new File(main.getDataFolder(), "schematic/" + schematicName);
            CuboidClipboard clipboard = SchematicFormat.getFormat(schematicFile).load(schematicFile);
            clipboard.rotate2D(180);
            return clipboard;
        }catch(IOException | DataException e) {
            main.getLogger().log(Level.SEVERE, "[WorldEditAPI] Failed to load schematic from file " + schematicName, e);
            throw new RuntimeException("An internal error occurred! Check console for stack trace.", e);
        }
    }
    public static void updateChunks(Region region, Player p) {
        World world = region.getWorld();
        if(world == null) {
            main.getLogger().log(Level.WARNING, "[WorldEditAPI] Failed to update chunks because region has null world");
            return;
        }
        org.bukkit.World bukkitWorld = Bukkit.getWorld(world.getName());
        List<Chunk> chunks = region.getChunks()
                .stream()
                .map(vec -> bukkitWorld.getChunkAt(vec.getBlockX(), vec.getBlockZ()))
                .collect(Collectors.toList());
        if(chunks.size() <= 0) return;
        List<net.minecraft.server.v1_8_R3.Chunk> nmsChunks = new ArrayList<>();
        PlayerConnection connection = ((CraftPlayer) p).getHandle().playerConnection;
        for(Chunk chunk : chunks) nmsChunks.add( ( (CraftChunk) chunk ).getHandle() );
        if(chunks.size() > 1)
            connection.sendPacket(new PacketPlayOutMapChunkBulk(nmsChunks));
        else {
            net.minecraft.server.v1_8_R3.Chunk chunk = nmsChunks.get(0);
            // From NMS PacketPlayOutMapChunkBulk
            boolean flag = !chunk.getWorld().worldProvider.o();
            connection.sendPacket(new PacketPlayOutMapChunk(chunk, flag, 65535));
        }
    }
}
