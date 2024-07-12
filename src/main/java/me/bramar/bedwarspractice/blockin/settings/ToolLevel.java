package me.bramar.bedwarspractice.blockin.settings;

import org.bukkit.Material;

public enum ToolLevel {
    WOOD(Material.WOOD_PICKAXE, Material.WOOD_AXE),
    STONE(Material.STONE_PICKAXE, Material.STONE_AXE),
    IRON(Material.IRON_PICKAXE, Material.IRON_AXE),
    GOLD(Material.GOLD_PICKAXE, Material.GOLD_AXE),
    DIAMOND(Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE);
    private final Material pickaxe, axe;
    ToolLevel(Material pickaxe, Material axe) {
        this.pickaxe = pickaxe;
        this.axe = axe;
    }

    public Material getPickaxe() {
        return pickaxe;
    }

    public Material getAxe() {
        return axe;
    }
}
