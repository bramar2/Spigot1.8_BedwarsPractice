package me.bramar.bedwarspractice.bridging;

import me.bramar.bedwarspractice.bridging.blockselector.BridgingBlock;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BridgingStats {
    private double averageTime = -1;
    private int averageTimeTop = 0;
    private int bridgeCount = 0;
    private double personalBest = -1;
    private int personalBestTop = 0;
    private int blocksPlaced = 0;
    private int coins = 0;
    private int coinsTop = 0;
    private int failedAttempts = 0;
    private String chosenBlock;
    private List<String> ownedBlocks = new ArrayList<>();

    private Map<String, PersonalBest> personalBests = new HashMap<>();


    private final UUID uuid;
    private final BridgingManager manager;
    public static final Pattern UUID_CHECK = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    BridgingStats(@NotNull BridgingManager manager, @NotNull String uuid) {
        if(!UUID_CHECK.matcher(uuid).matches()) throw new IllegalArgumentException("UUID is invalid");
        this.manager = manager;
        this.uuid = UUID.fromString(uuid);
        load();
        Bukkit.getScheduler().scheduleAsyncDelayedTask(manager.getPlugin(), this::updateTop);
    }

    // get methods

    public double getAverageTime() {
        return (double) Math.round(averageTime * 10000d) / 10000d;
    }
    public double getAverageTimeOriginal() {
        return averageTime;
    }

    public double getPersonalBest() {
        return personalBest;
    }

    public int getAverageTimeTop() {
        return averageTimeTop;
    }

    public int getPersonalBestTop() {
        return personalBestTop;
    }

    public double getSpecificPB(String groupName) {
        if(!personalBests.containsKey(groupName)) {
            personalBests.put(groupName, new PersonalBest(0.0d, 0));
            save("personal_bests");
            return 0.0d;
        }
        return personalBests.get(groupName).time;
    }

    public int getSpecificPBTop(String groupName) {
        if(!personalBests.containsKey(groupName)) {
            personalBests.put(groupName, new PersonalBest(0.0d, 0));
            save("personal_bests");
            return 0;
        }
        return personalBests.get(groupName).top;
    }

    public int getBridgeCount() {
        return bridgeCount;
    }

    public int getBlocksPlaced() {
        return blocksPlaced;
    }

    public int getCoins() {
        return coins;
    }

    public int getCoinsTop() {
        return coinsTop;
    }
    // set methods

    public void addCoins(int amount) {
        setCoins(coins + amount);
    }

    public void addPlacedBlocks(int amount) {
        setPlacedBlocks(blocksPlaced + amount);
    }
    public void setPlacedBlocks(int amount) {
        blocksPlaced = amount;
        save("blocks_placed");
    }
    public int resetPlacedBlocks() {
        int b = blocksPlaced;
        setPlacedBlocks(0);
        return b;
    }
    public void setCoins(int coins) {
        this.coins = coins;
        save("coins");
    }
    public int resetCoins() {
        int c = coins;
        setCoins(0);
        return c;
    }
    public double resetAverageTime() {
        double a = averageTime;
        this.averageTime = 0.0d;
        save("average_time");
        return a;
    }
    public void updateAverageTime(double newTime) {
        setAverageTime(
                ((averageTime * (double) (bridgeCount - 1) ) + newTime) / (double) bridgeCount
        );
    }
    public void setAverageTime(double newAverage) {
        averageTime = newAverage;
        save("average_time");
    }

    public int setBridgeCount(int newCount) {
        int bc = bridgeCount;
        this.bridgeCount = newCount;
        save("bridge_count");
        return bc;
    }

    public void setSpecificPB(String groupName, double time) {
        if(!personalBests.containsKey(groupName)) {
            personalBests.put(groupName, new PersonalBest(0.0d, 0));
        }else
            personalBests.get(groupName).time = time;

        save("personal_bests");
    }

    public void setSpecificPBTop(String groupName, int top) {
        if(!personalBests.containsKey(groupName)) {
            personalBests.put(groupName, new PersonalBest(0.0d, 0));
        }else
            personalBests.get(groupName).top = top;
    }

    public String getChosenBlock() {
        return chosenBlock;
    }
    public void setChosenBlock(String id) {
        chosenBlock = id;
        save("chosen_block");
    }

    public double setPersonalBest(double newBest) {
        double pb = personalBest;
        this.personalBest = newBest;
        save("personal_best");
        return pb;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public int setFailedAttempts(int failedAttempts) {
        int old = this.failedAttempts;
        this.failedAttempts = failedAttempts;
        save("failed_attempts");
        return old;
    }

    // update top (average time top and personal best top)
    private long lastUpdated = 0;
    public void updateTop() {
        // Top PB, etc (or leaderboards) are updated every 90 seconds at most (or they haven't been loaded yet)
        if(System.currentTimeMillis() > lastUpdated + 90000 || (topPB == null || topAT == null || topCoins == null)) {
            topSpecificPB = new HashMap<>();

            Map<UUID, BridgingStats> stats = manager.getAllStats();
            List<UUID> list = new ArrayList<>(stats.keySet());

            topPB = list.stream()
                    .filter(uuid -> stats.get(uuid).getPersonalBest() > 0.01d)
                    .sorted(getComparator(stats, BridgingStats::getPersonalBest))
                    .collect(Collectors.toList());

            topAT = list.stream()
                    .filter(uuid -> stats.get(uuid).getAverageTime() > 0.01d)
                    .sorted(getComparator(stats, BridgingStats::getAverageTime))
                    .collect(Collectors.toList());

            topCoins = list.stream()
                    .sorted(Comparator.comparingInt(uuid -> stats.get(uuid).coins).reversed())
                    .collect(Collectors.toList());

            // specific PBs

            Map<String, List<String>> areaGroups = manager.getAreaGroups();
            for(String groupName : areaGroups.keySet()) {
                List<UUID> top = list.stream()
                        .filter(uuid -> stats.get(uuid).getSpecificPB(groupName) > 0.01d)
                        .sorted(getComparator(stats, stat -> stat.getSpecificPB(groupName)))
                        .collect(Collectors.toList());

                topSpecificPB.put(groupName, top);
            }


            lastUpdated = System.currentTimeMillis();
        }
        personalBestTop = topPB.indexOf(uuid) + 1; // 0 if not found
        averageTimeTop = topAT.indexOf(uuid) + 1;
        coinsTop = topCoins.indexOf(uuid) + 1;

        manager.updated = true;
        save("personal_best_top", "average_time_top", "coins_top", "personal_bests");
    }
    private static Comparator<UUID> getComparator(Map<UUID, BridgingStats> mapper,
                                                  Function<BridgingStats, Double> function) {
        return (u1, u2) -> {
            BridgingStats s1 = mapper.get(u1);
            BridgingStats s2 = mapper.get(u2);
            double v1 = function.apply(s1);
            double v2 = function.apply(s2);

            if(v1 < 0 && v2 < 0) return 0;
            if(v1 < 0) return -1;
            if(v2 < 0) return 1;
            return Double.compare(v1, v2);
        };
    }

    protected void resetAndSave() {
        this.averageTime = 0d;
        this.personalBest = 0d;
        this.coins = 0;
        this.bridgeCount = 0;
        this.blocksPlaced = 0;

        this.personalBestTop = 0;
        this.averageTimeTop = 0;
        this.coinsTop = 0;
        this.failedAttempts = 0;
        this.ownedBlocks = new ArrayList<>();
        this.personalBests = new HashMap<>();
        saveAll();
    }
    public void load() {
        if(manager != null && uuid != null) {
            MemoryConfiguration section = (MemoryConfiguration) manager.config.getConfigurationSection("stats." + uuid);
            averageTime = section.getDouble("average_time");
            averageTimeTop = section.getInt("average_time_top");
            personalBest = section.getDouble("personal_best");
            personalBestTop = section.getInt("personal_best_top");
            blocksPlaced = section.getInt("blocks_placed");
            coins = section.getInt("coins");
            coinsTop = section.getInt("coins_top");
            bridgeCount = section.getInt("bridge_count");
            chosenBlock = section.getString("chosen_block");
            ownedBlocks = section.getStringList("owned_blocks");
            failedAttempts = section.getInt("failed_attempts");
            personalBests = new HashMap<>();
            ConfigurationSection pbConfig = section.getConfigurationSection("personal_bests");
            if(pbConfig != null)
                section.createSection("personal_bests");
            else {
                for(String key : pbConfig.getKeys(false)) {
                    ConfigurationSection pbSection = pbConfig.getConfigurationSection(key);
                    PersonalBest pb = new PersonalBest(pbSection.getDouble("time"), pbSection.getInt("top"));
                    personalBests.put(key, pb);
                }
            }
            manager.updated = true;
        }
    }

    public UUID getUniqueId() {
        return uuid;
    }
    private void saveAll() {
        ConfigurationSection section = manager.config.getConfigurationSection("stats." + uuid);
        section.set("average_time", averageTime);
        section.set("average_time_top", averageTimeTop);
        section.set("personal_best", personalBest);
        section.set("personal_best_top", personalBestTop);
        section.set("blocks_placed", blocksPlaced);
        section.set("coins", coins);
        section.set("coins_top", coinsTop);
        section.set("bridge_count", bridgeCount);
        section.set("chosen_block", chosenBlock);
        section.set("owned_blocks", ownedBlocks);
        section.set("failed_attempts", failedAttempts);
        if(section.contains("personal_bests"))
            section.set("personal_bests", null); // delete
        ConfigurationSection pbConfig = section.createSection("personal_bests");
        for(String key : personalBests.keySet()) {
            PersonalBest pb = personalBests.get(key);
            ConfigurationSection pbSection = pbConfig.createSection(key);
            pbSection.set("time", pb.time);
            pbSection.set("top", pb.top);
        }

        manager.updated = true;
    }
    private void save(String... key) {
        if(manager != null && uuid != null) {
            ConfigurationSection section = manager.config.getConfigurationSection("stats." + uuid);
            for(String str : key) {
                Object data;
                if(str.equalsIgnoreCase("average_time")) data = averageTime;
                else if(str.equalsIgnoreCase("average_time_top")) data = averageTimeTop;
                else if(str.equalsIgnoreCase("personal_best")) data = personalBest;
                else if(str.equalsIgnoreCase("personal_best_top")) data = personalBestTop;
                else if(str.equalsIgnoreCase("blocks_placed")) data = blocksPlaced;
                else if(str.equalsIgnoreCase("coins")) data = coins;
                else if(str.equalsIgnoreCase("coins_top")) data = coinsTop;
                else if(str.equalsIgnoreCase("bridge_count")) data = bridgeCount;
                else if(str.equalsIgnoreCase("chosen_block")) data = chosenBlock;
                else if(str.equalsIgnoreCase("owned_blocks")) data = ownedBlocks;
                else if(str.equalsIgnoreCase("failed_attempts")) data = failedAttempts;
                else if(str.equalsIgnoreCase("personal_bests")) {
                    if(section.contains("personal_bests"))
                        section.set("personal_bests", null); // delete
                    ConfigurationSection pbConfig = section.createSection("personal_bests");
                    for(String key2 : personalBests.keySet()) {
                        PersonalBest pb = personalBests.get(key2);
                        ConfigurationSection pbSection = pbConfig.createSection(key2);
                        pbSection.set("time", pb.time);
                        pbSection.set("top", pb.top);
                    }
                    continue;
                }
                else continue;
                section.set(str, data);
            }
            manager.updated = true;
        }
    }

    public List<String> getOwnedBlocks() {
        return ownedBlocks;
    }
    public void addOwnedBlock(String id) {
        ownedBlocks.add(id.toLowerCase());
        save("owned_blocks");
    }
    public boolean ownBlock(String id) {
        Bukkit.getScheduler().scheduleAsyncDelayedTask(manager.getPlugin(), () -> ownedBlocks = ownedBlocks.stream().map(String::toLowerCase).collect(Collectors.toList()), 1);
        save("owned_blocks");
        BridgingBlock b = manager.getBlock(id);
        if(b != null && b.isDefault()) return true;
        if(ownedBlocks == null) {
            ownedBlocks = new ArrayList<>();
            save("owned_blocks");
        }
        return ownedBlocks.contains(id.toLowerCase());
    }

    private static List<UUID> topPB, topAT, topCoins;
    private static Map<String, List<UUID>> topSpecificPB;

    public static Map<String, List<UUID>> getTopSpecificPB() {
        return topSpecificPB;
    }

    public static List<UUID> getTopPB() {
        return topPB;
    }
    public static List<UUID> getTopAverageTime() {
        return topAT;
    }
    public static List<UUID> getTopCoins() {
        return topCoins;
    }

    @Override
    public String toString() {
        return "BridgingStats{" +
                "averageTime=" + averageTime +
                ", averageTimeTop=" + averageTimeTop +
                ", bridgeCount=" + bridgeCount +
                ", personalBest=" + personalBest +
                ", personalBestTop=" + personalBestTop +
                ", blocksPlaced=" + blocksPlaced +
                ", coins=" + coins +
                ", coinsTop=" + coinsTop +
                ", failedAttempts=" + failedAttempts +
                ", chosenBlock='" + chosenBlock + '\'' +
                ", ownedBlocks=" + ownedBlocks +
                ", personalBests=" + personalBests +
                '}';
    }

    public Map<String, PersonalBest> getSpecificPBs() {
        return this.personalBests;
    }

    public static class PersonalBest {
        public double time;
        public int top;
        public PersonalBest(double time, int top) {
            this.time = time;
            this.top = top;
        }

        @Override
        public String toString() {
            return "PersonalBest{" +
                    "time=" + time +
                    ", top=" + top +
                    '}';
        }
    }
}
