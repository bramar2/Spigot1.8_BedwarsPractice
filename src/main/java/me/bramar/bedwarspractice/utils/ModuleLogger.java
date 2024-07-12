package me.bramar.bedwarspractice.utils;

import me.bramar.bedwarspractice.BedwarsPractice;
import org.bukkit.Bukkit;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

// Taken from PluginLogger
public class ModuleLogger extends Logger {
    private final String pluginPrefix;
    public ModuleLogger(String name) { // 'name' as in what shows up as {name} in: [...] [{name}]: {Message}
        super(BedwarsPractice.class.getCanonicalName(), null);
        pluginPrefix = "[" + name + "] ";
        setParent(Bukkit.getServer().getLogger());
        setLevel(Level.ALL);
    }

    @Override
    public void log(LogRecord logRecord) {
        logRecord.setMessage(pluginPrefix + logRecord.getMessage());
        super.log(logRecord);
    }
}
