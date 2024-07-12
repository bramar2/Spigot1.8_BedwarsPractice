package me.bramar.bedwarspractice.bridging;

import org.bukkit.scheduler.BukkitRunnable;

public class BridgingScoreboardTask extends BukkitRunnable {
    private final BridgingScoreboard sb;
    private final boolean animated;
    private int current;
    public BridgingScoreboardTask(BridgingScoreboard sb, boolean animated) {
        this.sb = sb;
        this.animated = animated;
        this.current = 0;
    }
    @Override
    public void run() {
        if(animated) sb.updateScoreboard(current++);
        else sb.updateScoreboard(0);
        if(animated && current == sb.content.size()) current = 0; // Reset
    }
}
