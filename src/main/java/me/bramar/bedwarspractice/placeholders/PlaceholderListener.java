package me.bramar.bedwarspractice.placeholders;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public interface PlaceholderListener {
    String onRequest(OfflinePlayer player, @NotNull String params);
}
