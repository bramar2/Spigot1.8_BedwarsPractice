package me.bramar.bedwarspractice.bridging;

import me.bramar.bedwarspractice.placeholders.PlaceholderListener;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class BridgingPlaceholders implements PlaceholderListener {
    private final BridgingManager manager;

    public BridgingPlaceholders(BridgingManager manager) {
        this.manager = manager;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String parameters) {
        String[] params = parameters.split("_");
        if(params.length > 1 && params[0].equalsIgnoreCase("bridging")) {
            // Stats
            // Normal stats:
            // %bedwars_bridging_[coins|personal_best|blocks_placed|average_time|personal_best|bridge_count|failed_attempts]_{player}%
            //
            // Rank stats:
            // %bedwars_bridging_rank_[coins|personal_best|average_time]_{player}%
            BridgingStats stats = manager.getStats(player.getUniqueId());
            if(params[1].equalsIgnoreCase("personal") &&
            params.length == 3 && params[2].equalsIgnoreCase("best")) return stats.getPersonalBest() + "";

            else if(params[1].equalsIgnoreCase("coins")) return stats.getCoins() + "";

            else if(params[1].equalsIgnoreCase("bridge") &&
            params.length == 3 && params[2].equalsIgnoreCase("count")) return stats.getBridgeCount() + "";

            else if(params[1].equalsIgnoreCase("blocks") &&
            params.length == 3 && params[2].equalsIgnoreCase("placed")) return stats.getBlocksPlaced() + "";

            else if(params[1].equalsIgnoreCase("average") &&
            params.length == 3 && params[2].equalsIgnoreCase("time")) return stats.getAverageTime() + "";

            else if(params[1].equalsIgnoreCase("failed") && params.length == 3 &&
            params[2].equalsIgnoreCase("attempts")) return stats.getFailedAttempts() + "";

            if(params.length == 4 && params[3].equalsIgnoreCase("rank")) {
                if("personalbest".equalsIgnoreCase(params[1] + params[2])) return stats.getPersonalBestTop() + "";
                if("averagetime".equalsIgnoreCase(params[1] + params[2])) return stats.getAverageTimeTop() + "";
            }
            if(params.length == 3 && params[2].equalsIgnoreCase("rank") &&
                    params[1].equalsIgnoreCase("coins")) return stats.getCoinsTop() + "";

            // Other players
            // Normal stats:
            // %bedwars_bridging_other_[coins|personal_best|blocks_placed|average_time|personal_best|bridge_count|failed_attempts]_{player}%
            //
            // Rank stats:
            // %bedwars_bridging_other_rank_[coins|personal_best|average_time]_{player}%
            if(params.length >= 4 && params[1].equalsIgnoreCase("other")) {
                stats = manager.getStats(params.length == 4 ? params[3] :
                        (params.length == 5 ? params[4] :
                                params[5])).getKey();
                if(stats != null) {
                    if(params.length == 4 && params[2].equalsIgnoreCase("coins")) return stats.getCoins() + "";
                    else if(params.length == 5) {
                        String combined = params[2] + params[3];
                        if(combined.equalsIgnoreCase("personalbest")) return stats.getPersonalBest() + "";
                        else if(combined.equalsIgnoreCase("bridgecount")) return stats.getBridgeCount() + "";
                        else if(combined.equalsIgnoreCase("blocksplaced")) return stats.getBlocksPlaced() + "";
                        else if(combined.equalsIgnoreCase("averagetime")) return stats.getAverageTime() + "";
                        else if(combined.equalsIgnoreCase("rankcoins")) return stats.getCoinsTop() + "";
                        else if(combined.equalsIgnoreCase("failedattempts")) return stats.getFailedAttempts() + "";
                    }else if(params[2].equalsIgnoreCase("rank")) {
                        String combined = params[3] + params[4];
                        if(combined.equalsIgnoreCase("personalbest")) return stats.getPersonalBestTop() + "";
                        else if(combined.equalsIgnoreCase("averagetime")) return stats.getAverageTimeTop() + "";
                    }
                }
            }

            // Leaderboards
            // Get player name of #X -> %bedwars_bridging_top_player_[coins|average_time|personal_best]_X%
            // Get value of #X (200 coins, 300s, etc) -> %bedwars_bridging_top_value_[coins|average_time|personal_best]_X%
            if(params.length > 3 && params[1].equalsIgnoreCase("top")) {
                boolean a, b, c;
                if(params.length == 5 && isInt(params[4])) {
                    if((a = params[3].equalsIgnoreCase("coins")) && params[2].equalsIgnoreCase("player")) {
                        int i = Integer.parseInt(params[4]) - 1;
                        if(i < 0) return null;
                        UUID uuid = safeGet(BridgingStats.getTopCoins(), i);
                        if(uuid == null) return "No one!";
                        Player p = Bukkit.getPlayer(uuid);
                        if(p != null) return p.getName();
                        OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
                        if(off != null) return off.getName();
                        return uuid.toString();
                    }else if(a && params[2].equalsIgnoreCase("value")) {
                        BridgingStats output = manager.getStats(safeGet(BridgingStats.getTopCoins(), Integer.parseInt(params[4]) - 1));
                        return output.getCoins() + "";
                    }
                }else if(params.length == 6 && isInt(params[5])) {
                    if((b = "personalbest".equalsIgnoreCase(params[3] + params[4])) && params[2].equalsIgnoreCase("player")) {
                        int i = Integer.parseInt(params[5]) - 1;
                        if(i < 0) return null;
                        UUID uuid = safeGet(BridgingStats.getTopPB(), i);
                        if(uuid == null) return "No one!";
                        Player p = Bukkit.getPlayer(uuid);
                        if(p != null) return p.getName();
                        OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
                        if(off != null) return off.getName();
                        return uuid.toString();
                    }else if((c = "averagetime".equalsIgnoreCase(params[3] + params[4])) && params[2].equalsIgnoreCase("player")) {
                        int i = Integer.parseInt(params[5]) - 1;
                        if(i < 0) return null;
                        UUID uuid = safeGet(BridgingStats.getTopAverageTime(), i);
                        if(uuid == null) return "No one!";
                        Player p = Bukkit.getPlayer(uuid);
                        if(p != null) return p.getName();
                        OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
                        if(off != null) return off.getName();
                        return uuid.toString();
                    }
                    if((a = params[2].equalsIgnoreCase("value")) && b) {
                        BridgingStats output = manager.getStats(safeGet(BridgingStats.getTopCoins(), Integer.parseInt(params[5]) - 1));
                        if(output == null) return "0";
                        return output.getPersonalBest() + "";
                    }else if(a && c) {
                        BridgingStats output = manager.getStats(safeGet(BridgingStats.getTopCoins(), Integer.parseInt(params[5]) - 1));
                        if(output == null) return "0";
                        return output.getAverageTime() + "";
                    }
                }
            }

            // Current Area Info
            // %bedwars_bridging_area_[name|blocks|time]%
            if(params.length == 3 && params[1].equalsIgnoreCase("area") && player instanceof Player) {
                BridgingArea area = manager.getArea((Player) player);
                if(area != null) {
                    if(params[2].equalsIgnoreCase("name")) return area.getAreaName();
                    if(params[2].equalsIgnoreCase("blocks")) return area.getBlockAmount() + "";
                    if(params[2].equalsIgnoreCase("time")) return area.getSecondsTimer() + "";
                }
            }
            // Player in area
            // %bedwars_bridging_areaplayer_{area}_{stat}
            if(params[1].equalsIgnoreCase("areaplayer") && params.length >= 4) {
                BridgingArea area = manager.getArea(params[2]);
                Player p;
                if(area != null && (p = area.getPlayer()) != null) {
                    stats = area.getStats();
                    if(stats == null) stats = manager.getStats(p.getUniqueId()); // NotNull
                    if(params.length == 4 && params[3].equalsIgnoreCase("coins"))
                        return stats.getCoins() + "";
                    else if(params.length == 5) {
                        String combined = params[3] + params[4];
                        if(combined.equalsIgnoreCase("personalbest")) return stats.getPersonalBest() + "";
                        if(combined.equalsIgnoreCase("blocksplaced")) return stats.getBlocksPlaced() + "";
                        if(combined.equalsIgnoreCase("averagetime")) return stats.getAverageTime() + "";
                        if(combined.equalsIgnoreCase("bridgecount")) return stats.getBridgeCount() + "";
                        if(combined.equalsIgnoreCase("failedattempts")) return stats.getFailedAttempts() + "";
                        if(combined.equalsIgnoreCase("rankcoins")) return stats.getCoinsTop() + "";
                    }else if(params.length == 6 && params[3].equalsIgnoreCase("rank")) {
                        String combined = params[4] + params[5];
                        if(combined.equalsIgnoreCase("personalbest")) return stats.getPersonalBestTop() + "";
                        if(combined.equalsIgnoreCase("averagetime")) return stats.getAverageTimeTop() + "";
                    }

                }
            }
        }
        return null;
    }
    private <E> @Nullable E safeGet(List<E> list, int index) {
        if(index < 0 || index >= list.size()) return null;
        return list.get(index);
    }
    private boolean isInt(String str) {
        for(char c : str.toCharArray()) {
            if(!Character.isDigit(c)) return false;
        }
        return true;
    }
}
