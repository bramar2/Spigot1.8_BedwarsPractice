package me.bramar.bedwarspractice.bridging.blockselector;

import me.bramar.bedwarspractice.BedwarsPractice;
import me.bramar.bedwarspractice.bridging.BridgingArea;
import me.bramar.bedwarspractice.bridging.BridgingManager;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A simple Block Selector NPC trait for right-clicking
 */
public class BlockSelectorTrait extends Trait {
    private final BridgingManager manager;

    public BlockSelectorTrait() {
        super("blockselector");
        this.manager = JavaPlugin.getPlugin(BedwarsPractice.class).getBridgingManager();
    }


    @EventHandler
    public void onRightClick(NPCRightClickEvent e) {
        if(e.getNPC() == this.npc) {
            Player p = e.getClicker();
            BridgingArea a = manager.getArea(p);
            if(a != null) manager.openBlockSelector(p);
        }
    }
}
