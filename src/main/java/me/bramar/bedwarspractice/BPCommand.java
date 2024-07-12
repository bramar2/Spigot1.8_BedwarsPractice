package me.bramar.bedwarspractice;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class BPCommand implements CommandExecutor, TabCompleter {
    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player) {
            Player p = (Player) sender;
            String mainPerm = getPermission();
            if(mainPerm != null && !p.hasPermission(mainPerm)) p.sendMessage(getPermissionMessage());
            else {
                try {
                    if(args.length == 0) p.sendMessage(ChatColor.RED + "Type /" + getName() + " help for commands");
                    else {
                        String mainArg = args[0];
                        if(mainArg.equalsIgnoreCase("help")) showHelp(p);
                        else {
                            List<String> valid = validArguments();
                            if(valid != null) {
                                int index = valid.indexOf(mainArg);
                                if(index == -1) {
                                    p.sendMessage(ChatColor.RED + "Invalid argument! Type /" + getName() + " help for commands");
                                    return true;
                                }
                                String perm = getArgPermission(index);
                                if(perm != null && !p.hasPermission(perm)) {
                                    p.sendMessage(getPermissionMessage());
                                    return true;
                                }
                            }
                            onCommand(p, mainArg, args);
                        }
                    }
                }catch(Exception e1) {
                    if(printErrors()) e1.printStackTrace();
                    p.sendMessage(ChatColor.RED + "ERROR: " + e1.getMessage());
                }
            }
        }else if(sender instanceof ConsoleCommandSender) {
            ConsoleCommandSender c = (ConsoleCommandSender) sender;
            try {
                if(allowsConsole()) {
                    if(args.length == 0) c.sendMessage("Type /" + getName() + " help for commands");
                    else {
                        String mainArg = args[0];
                        if(mainArg.equalsIgnoreCase("help")) showHelp(c);
                        else {
                            List<String> valid = validArguments();
                            if(valid != null && !valid.contains(mainArg)) {
                                c.sendMessage("Invalid argument! Type /" + getName() + " help for commands");
                                return true;
                            }
                            onConsoleCommand(c, mainArg, args);
                        }
                    }
                }
            }catch(Exception e1) {
                if(printErrors()) e1.printStackTrace();
                c.sendMessage("ERROR: " + e1.getClass().getName() + ": " + e1.getMessage());
            }
        }else sender.sendMessage("This command only works as a player and console");
        return true;
    }

    private void showHelp(CommandSender sender) {
        StringBuilder helpMsg = new StringBuilder();
        if(sender instanceof Player) {
            Player p = (Player) sender;
            List<String> help = getHelp(p);
            (help != null ? help : getHelp())
                    .forEach((str) -> helpMsg.append(str).append("\n"));
        }else getHelp().forEach((str) -> helpMsg.append(str).append("\n"));
        sender.sendMessage(helpMsg.substring(0, helpMsg.length() - 2));
    }

    @Override
    public final List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return (args.length == 0) ?
                (onEmptyTabComplete(sender)) :
                (onTabComplete(sender, args[0], args));
    }

    public abstract String getCommand();
    public abstract String getName();
    public abstract List<String> getHelp();
    public @Nullable List<String> getHelp(Player p) {
        return null;
    }
    public @Nullable String getPermission() {return null;} // Optional. Recommended to use validArguments with getArgPermission
    public abstract String getPermissionMessage();
    public @Nullable List<String> validArguments() {return null;}
    //
    public @Nullable String getArgPermission(int index) {
        return null;
    }
    public boolean printErrors() { return false; }

    //

    public abstract void onCommand(Player p, String mainArg, String[] args) throws Exception;
    public void onConsoleCommand(ConsoleCommandSender sender, String mainArg, String[] args) throws Exception {}
    public boolean allowsConsole() { return false; }
    public final void register() {
        PluginCommand cmd = Bukkit.getPluginCommand(getCommand());
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
    }
    public List<String> onEmptyTabComplete(CommandSender sender) {
        List<String> validArgs = validArguments();
        if(validArgs != null) {
            if(sender instanceof Player) {
                List<String> list = new ArrayList<>();
                Player p = (Player) sender;
                for(int i = 0; i < validArgs.size(); i++) {
                    String perm = getArgPermission(i);
                    if(perm == null || p.hasPermission(perm)) list.add(validArgs.get(i));
                }
                return list;
            }else return validArgs;
        }
        return null;
    }
    public List<String> onTabComplete(CommandSender sender, String mainArg, String[] args) {
        return null;
    }
}
