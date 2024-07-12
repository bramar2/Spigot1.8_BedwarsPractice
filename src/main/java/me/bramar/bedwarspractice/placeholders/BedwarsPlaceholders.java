package me.bramar.bedwarspractice.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BedwarsPlaceholders extends PlaceholderExpansion {
    private final List<PlaceholderListener> listeners = new ArrayList<>();
    @Override
    public @NotNull String getIdentifier() {
        return "bedwars";
    }

    @Override
    public @NotNull String getAuthor() {
        return "bramar";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        for(PlaceholderListener listener : listeners) {
            try {
                String output = listener.onRequest(player, params);
                if(output != null) return output;
            }catch(Exception ignored) {}
        }
        return null;
    }
    public void register(PlaceholderListener listener) {
        listeners.add(listener);
    }
}
