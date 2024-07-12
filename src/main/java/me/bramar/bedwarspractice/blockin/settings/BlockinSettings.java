package me.bramar.bedwarspractice.blockin.settings;

import me.bramar.bedwarspractice.blockin.BlockinManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class BlockinSettings {
    private final ConfigurationSection config;
    private final BlockinManager manager;
    //
    private ToolLevel pickaxeType = ToolLevel.WOOD, axeType = pickaxeType;
    private int hasteLevel = 0; // Haste effect
    private int pickaxeEff = 1, axeEff = 1; // Efficiency Level
    // Selected block
    private Material selectedBlock = Material.WOOL;
    private int selectedBlockData = 0;
    //
    private boolean takeDamage = false;

    public BlockinSettings(BlockinManager manager, ConfigurationSection section) {
        this.manager = manager;
        this.config = section;
        load();
    }
    private void load() {
        try {
            ToolLevel lvl = getEnum("pickaxe_type", ToolLevel.class);
            Object obj;
            if(lvl != null) pickaxeType = lvl;
            lvl = getEnum("axe_type", ToolLevel.class);
            if(lvl != null) axeType = lvl;
            if((obj = config.get("haste")) instanceof Integer) hasteLevel = (int) obj;
            if((obj = config.get("pickaxe_eff")) instanceof Integer) pickaxeEff = (int) obj;
            if((obj = config.get("axe_eff")) instanceof Integer) axeEff = (int) obj;

            Material mat = getEnum("selected_block_type", Material.class);
            if(mat != null) {
                selectedBlock = mat;
                selectedBlockData = 0;
                if((obj = config.get("selected_block_data")) instanceof Number)
                    selectedBlockData = ((Number) obj).intValue();
            }

            if((obj = config.get("take_damage")) instanceof Boolean) takeDamage = (boolean) obj;
        }catch(Exception ignored) {}
    }

    private <E extends Enum<E>> E getEnum(String path, Class<E> clazz) {
        String str = config.getString(path);
        if(str == null) return null;
        try {
            return Enum.valueOf(clazz, str);
        }catch(Exception ignored) {}
        return null;
    }

    public void save(String... keys) {
        for(String str : keys) {
            Object data;
            if(str.equalsIgnoreCase("pickaxe_type")) data = pickaxeType;
            else if(str.equalsIgnoreCase("axe_type")) data = axeType;
            else if(str.equalsIgnoreCase("pickaxe_eff")) data = pickaxeEff;
            else if(str.equalsIgnoreCase("axe_eff")) data = axeEff;
            else if(str.equalsIgnoreCase("haste")) data = hasteLevel;
            else if(str.equalsIgnoreCase("selected_block_type")) data = selectedBlock;
            else if(str.equalsIgnoreCase("selected_block_data")) data = selectedBlockData;
            else if(str.equalsIgnoreCase("take_damage")) data = takeDamage;
            else continue;
            if(data instanceof Enum) data = ((Enum) data).name();
            config.set(str, data);
        }
        manager.updated = true;
    }

    public ToolLevel getPickaxeType() {
        return pickaxeType;
    }

    public ToolLevel getAxeType() {
        return axeType;
    }

    public int getPickaxeEff() {
        return pickaxeEff;
    }

    public int getAxeEff() {
        return axeEff;
    }

    public int getHasteLevel() {
        return hasteLevel;
    }

    public int getBlockData() {
        return selectedBlockData;
    }

    public Material getBlockType() {
        return selectedBlock;
    }

    public boolean takeDamage() {
        return takeDamage;
    }

    public void setTakeDamage(boolean takeDamage) {
        this.takeDamage = takeDamage;
        save("take_damage");
    }

    public void setBlock(Material selectedBlock) {
        this.selectedBlock = selectedBlock;
        save("selected_block_type");
    }

    public void setBlockData(int selectedBlockData) {
        this.selectedBlockData = selectedBlockData;
        save("selected_block_data");
    }

    public void setAxeEff(int axeEff) {
        this.axeEff = axeEff;
        save("axe_eff");
    }

    public void setPickaxeEff(int pickaxeEff) {
        this.pickaxeEff = pickaxeEff;
        save("pickaxe_eff");
    }

    public void setAxeType(ToolLevel axeType) {
        this.axeType = axeType;
        save("axe_type");
    }

    public void setHasteLevel(int hasteLevel) {
        this.hasteLevel = hasteLevel;
        save("haste");
    }

    public void setPickaxeType(ToolLevel pickaxeType) {
        this.pickaxeType = pickaxeType;
        save("pickaxe_type");
    }
}
