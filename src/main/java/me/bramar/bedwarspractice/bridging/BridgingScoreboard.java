package me.bramar.bedwarspractice.bridging;

import me.bramar.bedwarspractice.utils.ScoreboardWrapper;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BridgingScoreboard extends ScoreboardWrapper {
    private final Player p;
    private final BridgingManager manager;
    public final List<List<String>> content;
    private List<String> titles;
    public BridgingScoreboard(BridgingManager manager, Player p) {
        super(manager.getScoreboardTitle(p));
        ConfigurationSection scoreboardSection = manager.config.getConfigurationSection("scoreboard");
        this.manager = manager;
        this.p = p;
        isAnimated = false;
        this.content = new ArrayList<>();
        content.add(scoreboardSection.getStringList("content"));
        updateScoreboard(0);
    }
    public BridgingScoreboard(BridgingManager manager, Player p, List<List<String>> contents, List<String> titles) {
        super(manager.getScoreboardTitle(p));
        this.manager = manager;
        this.p = p;
        this.content = contents;
        this.titles = titles;
        isAnimated = true;
        updateScoreboard(0);
    }
    // index doesn't matter if !isAnimated
    public void updateScoreboard(int index) {
        if(isAnimated) {
            setTitle(applyPlaceholders(titles.get(index)));
            List<String> list = content.get(index);
            List<String> applied = list.stream().map(this::applyPlaceholders).collect(Collectors.toList()); // Applied to PlaceholderAPI
            setLines(applied);
        }else {
            List<String> list = content.get(0);
            List<String> applied = list.stream().map(this::applyPlaceholders).collect(Collectors.toList()); // Applied to PlaceholderAPI
            setLines(applied);
        }
    }
    private String applyPlaceholders(String str) {
        return manager.hasPlaceholderAPI() ? PlaceholderAPI.setPlaceholders(p, str) : str;
    }
    public final boolean isAnimated;
}
