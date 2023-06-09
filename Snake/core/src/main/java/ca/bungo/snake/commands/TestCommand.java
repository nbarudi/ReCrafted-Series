package ca.bungo.snake.commands;

import ca.bungo.snake.Snake;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TestCommand extends Command {
    public TestCommand(@NotNull String name) {
        super(name);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        if(Snake.getInstance().abstractedHandler == null) return false;
        if(!(sender instanceof Player player)) return false;

        Entity mountable = Snake.getInstance().abstractedHandler.createMountableEntity(EntityType.SHEEP, player.getWorld());
        mountable.setInvulnerable(true);
        mountable.spawnAt(player.getLocation());
        mountable.addPassenger(player);

        return false;
    }
}
