package ca.bungo.snake.commands;

import ca.bungo.snake.Snake;
import ca.bungo.snake.types.Arena;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SnakeCommand extends Command {


    public SnakeCommand(@NotNull String name) {
        super(name);
        this.description = "Main Snake Command";
        this.usageMessage = "/" + this.getName() + " <setspawn|join|leave> [Arena]";
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if(!(sender instanceof Player player)) return false;

        if(args.length >= 1){
            switch (args[0].toLowerCase()){
                case "setspawn":
                    Location location = player.getLocation();
                    FileConfiguration config = Snake.getInstance().getConfig();
                    config.set("spawn.world", location.getWorld().getName());
                    config.set("spawn.x", location.getBlockX());
                    config.set("spawn.y", location.getBlockY());
                    config.set("spawn.z", location.getBlockZ());

                    Snake.getInstance().saveConfig();

                    player.sendMessage(LinearComponents.linear(NamedTextColor.GREEN, Component.text("Set Spawnpoint!")));
                    return false;
                case "join":
                    if(args.length != 2){
                        player.sendMessage(LinearComponents.linear(NamedTextColor.DARK_RED, Component.text("Error: Invalid Usage, " + this.usageMessage)));
                        return false;
                    }
                    String arenaName = args[1];

                    if(Snake.getInstance().arenaManager.joinArena(player, arenaName)){
                        player.sendMessage(LinearComponents.linear(NamedTextColor.GREEN, Component.text("Joined arena "),
                                NamedTextColor.YELLOW, Component.text(arenaName)));
                    } else {
                        player.sendMessage(LinearComponents.linear(NamedTextColor.DARK_RED, Component.text("Error: Arena was not found!")));
                    }
                    return false;
                case "leave":
                    if(Snake.getInstance().arenaManager.leaveArena(player)){
                        player.sendMessage(LinearComponents.linear(NamedTextColor.GREEN, Component.text("Left the arena!")));
                    } else {
                        player.sendMessage(LinearComponents.linear(NamedTextColor.DARK_RED, Component.text("Error: You're not in an arena!")));
                    }
                    return false;
            }
        }
        return false;
    }


    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {

        if(args.length == 1){
            return List.of("join", "leave", "setspawn");
        }
        else if (args[0].equalsIgnoreCase("join")) {
            ConfigurationSection section = Snake.getInstance().getConfig().getConfigurationSection("arenas");
            if(section == null)
                return super.tabComplete(sender, alias, args);
            return new ArrayList<>(section.getKeys(false));
        }

        return super.tabComplete(sender, alias, args);
    }
}
