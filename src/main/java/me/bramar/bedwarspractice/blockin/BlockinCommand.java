package me.bramar.bedwarspractice.blockin;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.bramar.bedwarspractice.BPCommand;
import me.bramar.bedwarspractice.BedwarsPractice;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class BlockinCommand extends BPCommand {
    private final BedwarsPractice main;
    public BlockinCommand(BedwarsPractice main) {
        this.main = main;
    }

    @Override
    public String getCommand() {
        return "bedwarsblockin";
    }

    @Override
    public String getName() {
        return "blockin";
    }

    @Override
    public List<String> getHelp() {
        return Arrays.asList("/blockin <worldguard>: This is test");
    }

    @Override
    public @Nullable List<String> getHelp(Player p) {
        return super.getHelp(p);
    }

    @Override
    public String getPermissionMessage() {
        return ChatColor.RED + "No permission!";
    }

    @Override
    public @Nullable List<String> validArguments() {
        return super.validArguments();
    }

    @Override
    public @Nullable String getArgPermission(int index) {
        return super.getArgPermission(index);
    }

    @Override
    public void onCommand(Player p, String mainArg, String[] args) throws Exception {
        p.sendMessage("blockin test running");
        p.sendMessage("do arg after region to enableQueue after task");
        if(args.length < 2) {
            p.sendMessage("you need worldguard region name as arg. and if there is another arg after it, it will do commit() instead of flushQueue()");
            return;
        }
        p.sendMessage("region inputted: '" + args[1] + "'");
        WorldEdit we = WorldEdit.getInstance();
        WorldGuardPlugin wg = WorldGuardPlugin.inst();
        LocalWorld world = BukkitUtil.getLocalWorld(p.getWorld());
        ProtectedRegion region = wg.getRegionManager(p.getWorld()).getRegion(args[1]);
        if(region == null) throw new NullPointerException("invalid region");
        EditSession session = we.getEditSessionFactory().getEditSession(world, -1);
        session.enableQueue();
        System.out.println("edit session started");
        BaseBlock air = new BaseBlock(BlockID.AIR);
        int y1 = region.getMinimumPoint().getBlockY();
        int y2 = region.getMaximumPoint().getBlockY();
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        session.setBlocks(new Polygonal2DRegion(world, region.getPoints(), minY, maxY), air);
        System.out.println("blocks setup!");
        if(args.length > 2) session.flushQueue(); // Set to AIR
        System.out.println("session flushed (has been set to air)");
        Bukkit.getScheduler().scheduleSyncDelayedTask(JavaPlugin.getPlugin(BedwarsPractice.class), () -> {
            // Undo 3 seconds later
            if(args.length > 2) session.enableQueue();
            session.undo(we.getEditSessionFactory().getEditSession(world, -1));
            session.flushQueue();
            p.sendMessage("edit session flushed (2), 3s has passed. session undo-ed");
        }, 3 * 20);
        p.sendMessage("blockin test done (3s might be a while)");
    }

    @Override
    public boolean printErrors() {
        return true;
    }

    @Override
    public void onConsoleCommand(ConsoleCommandSender sender, String mainArg, String[] args) throws Exception {
        super.onConsoleCommand(sender, mainArg, args);
    }

    @Override
    public boolean allowsConsole() {
        return super.allowsConsole();
    }

    @Override
    public List<String> onEmptyTabComplete(CommandSender sender) {
        return super.onEmptyTabComplete(sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String mainArg, String[] args) {
        return super.onTabComplete(sender, mainArg, args);
    }
}
