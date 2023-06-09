package ca.bungo.snake.commands;

import ca.bungo.snake.Snake;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArenaCommand extends Command {
    public ArenaCommand(@NotNull String name) {
        super(name);
        this.description = "Main command for managing Snake areans!";
        this.usageMessage = "/" + name + " <create|delete|setspawn|reload> ";
        this.setPermission("snake.arena.manage");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        if(!(sender instanceof Player player)) return false;

        if(args.length == 0){
            //ToDo: List command arguments
            player.sendMessage(LinearComponents.linear(NamedTextColor.BLUE, Component.text("Usage: " + this.usageMessage)));
        }else{
            String setting = args[0].toLowerCase();
            FileConfiguration config = Snake.getInstance().getConfig();
            switch (setting){
                case "create":
                    if(args.length != 2){
                        player.sendMessage(LinearComponents.linear(NamedTextColor.DARK_RED, Component.text("Error: Invalid Usage, create <ArenaName>")));
                        return false;
                    }
                    String arenaName = args[1];

                    LocalSession session =  WorldEdit.getInstance().getSessionManager().getIfPresent(BukkitAdapter.adapt(player));
                    if(session == null){
                        player.sendMessage(LinearComponents.linear(NamedTextColor.DARK_RED, Component.text("Error: Please make a WorldEdit selection for your arena!")));
                        return false;
                    }

                    CuboidRegion zone;
                    try {
                         zone = session.getSelection().getBoundingBox();
                    } catch (IncompleteRegionException e) {
                        player.sendMessage(LinearComponents.linear(NamedTextColor.DARK_RED, Component.text("Error: Please make a WorldEdit selection for your arena!")));
                        return false;
                    }

                    BlockVector3 startPos = zone.getPos1();
                    BlockVector3 endPos = zone.getPos2();


                    if(config.getConfigurationSection("arenas." + arenaName) != null){
                        player.sendMessage(LinearComponents.linear(NamedTextColor.DARK_RED, Component.text("Error: Arena already exists!")));
                        return false;
                    }

                    ConfigurationSection localArena = config.createSection("arenas." + arenaName);
                    localArena.set("world", player.getWorld().getName());

                    localArena.set("startPos.x", startPos.getX());
                    localArena.set("startPos.y", startPos.getY());
                    localArena.set("startPos.z", startPos.getZ());

                    localArena.set("endPos.x", endPos.getX());
                    localArena.set("endPos.y", endPos.getY());
                    localArena.set("endPos.z", endPos.getZ());

                    Snake.getInstance().saveConfig();
                    player.sendMessage(LinearComponents.linear(NamedTextColor.GREEN, Component.text("Successfully created arena "), NamedTextColor.YELLOW, Component.text(arenaName)));
                    player.sendMessage(LinearComponents.linear(NamedTextColor.GOLD, Component.text("Make sure you set the spawnpoint inside the arena!")));
                    return false;
                case "delete":
                    if(args.length != 2){
                        player.sendMessage(LinearComponents.linear(NamedTextColor.DARK_RED, Component.text("Error: Invalid Usage, delete <ArenaName>")));

                        return false;
                    }
                    arenaName = args[1];
                    if(config.getConfigurationSection("arenas." + arenaName) == null){
                        player.sendMessage(LinearComponents.linear(NamedTextColor.DARK_RED, Component.text("Error: Arena does not exist!")));
                        return false;
                    }

                    config.set("arenas." + arenaName, null);
                    Snake.getInstance().saveConfig();
                    player.sendMessage(LinearComponents.linear(NamedTextColor.RED, Component.text("Successfully removed arena "), NamedTextColor.YELLOW, Component.text(arenaName)));
                    return false;
                case "setspawn":
                    if(args.length != 2){
                        player.sendMessage(LinearComponents.linear(NamedTextColor.DARK_RED, Component.text("Error: Invalid Usage, setspawn <ArenaName>")));
                        return false;
                    }
                    arenaName = args[1];
                    Location location = player.getLocation();

                    config.set("arenas." + arenaName + ".spawn.world", location.getWorld().getName());
                    config.set("arenas." + arenaName + ".spawn.x", location.getBlockX());
                    config.set("arenas." + arenaName + ".spawn.y", location.getBlockY());
                    config.set("arenas." + arenaName + ".spawn.z", location.getBlockZ());

                    Snake.getInstance().saveConfig();

                    player.sendMessage(LinearComponents.linear(NamedTextColor.YELLOW, Component.text("Set Spawnpoint for arena "), NamedTextColor.AQUA, Component.text(arenaName)));
                    return false;
                case "reload":
                    Snake.getInstance().arenaManager.reloadArenas();
                    player.sendMessage(LinearComponents.linear(NamedTextColor.GREEN, Component.text("Reloaded arenas!")));
                    return false;
                case "start":
                    if(args.length != 2){
                        player.sendMessage(LinearComponents.linear(NamedTextColor.DARK_RED, Component.text("Error: Invalid Usage, start <ArenaName>")));
                        return false;
                    }
                    arenaName = args[1];
                    Snake.getInstance().arenaManager.forceStart(arenaName);
                    player.sendMessage(Component.text("Attempting to start the game!").color(NamedTextColor.GREEN));
                    return false;
                case "stop":
                    if(args.length != 2){
                        player.sendMessage(LinearComponents.linear(NamedTextColor.DARK_RED, Component.text("Error: Invalid Usage, start <ArenaName>")));
                        return false;
                    }
                    arenaName = args[1];
                    Snake.getInstance().arenaManager.forceStop(arenaName);
                    player.sendMessage(Component.text("Attempting to start the game!").color(NamedTextColor.GREEN));
                    return false;
            }
        }
        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {

        if(args.length == 1){
            return List.of("create", "delete", "setspawn", "reload", "start", "stop");
        }
        else if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("setspawn")
                || args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("stop")) {
            ConfigurationSection section = Snake.getInstance().getConfig().getConfigurationSection("arenas");
            if(section == null)
                return super.tabComplete(sender, alias, args);
            return new ArrayList<>(section.getKeys(false));
        }

        return super.tabComplete(sender, alias, args);
    }
}
