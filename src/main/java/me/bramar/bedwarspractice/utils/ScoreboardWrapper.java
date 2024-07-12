package me.bramar.bedwarspractice.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;

/**
 * ScoreboardWrapper is a class that wraps Bukkit Scoreboard API
 * and makes your life easier.
 */
public class ScoreboardWrapper {

    public static final int MAX_LINES = 16;

    private final Scoreboard scoreboard;
    private final Objective objective;

    private final List<String> modifies = new ArrayList<>(MAX_LINES);

    /**
     * Instantiates a new ScoreboardWrapper with a default title.
     */
    public ScoreboardWrapper(String title) {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective(title, "dummy");
        objective.setDisplayName(title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    /**
     * Sets the scoreboard title.
     */
    public void setTitle(String title) {
        objective.setDisplayName(title);
    }

    /**
     * Modifies the line with §r strings in the way to add
     * a line equal to another.
     */
    private String getLineCoded(String line) {
        String result = line;
        while (modifies.contains(result))
            result += ChatColor.RESET;
        return result.substring(0, Math.min(40, result.length()));
    }

    /*
     * All other methods manually removed and replaced with this one.
     */
    public void setLines(List<String> lines) {
        if(lines == null)
            throw new NullPointerException("List cannot be null! Use Collections#emptyList() to make the scoreboard empty.");
        if(lines.size() > MAX_LINES)
            throw new IndexOutOfBoundsException("The list size cannot be higher than 16");
        if(lines.isEmpty()) return; // Already cleared by above
        String[] linesArr = new String[lines.size()];
        int[] scores = new int[lines.size()];
        List<String> modifies = new ArrayList<>();
        for(int index = 0; index < lines.size(); index++) {
            String modified = getLineCoded(lines.get(index));
            modifies.add(modified);
            linesArr[index] = modified;
            scores[index] = (-index + lines.size() - 1);
        }
        this.modifies.forEach(scoreboard::resetScores); // Reset scores
        this.modifies.clear();
        for(int i = 0; i < linesArr.length; i++) {
            objective.getScore(linesArr[i]).setScore(scores[i]);
        }
        this.modifies.addAll(modifies);
    }

    /**
     * Gets the Bukkit Scoreboard.
     */
    public Scoreboard getScoreboard() {
        return scoreboard;
    }
}