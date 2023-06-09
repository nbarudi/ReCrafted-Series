package ca.bungo.snake.abstracted;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public abstract class AbstractedHandler {
    protected AbstractedLink helper;

    public AbstractedHandler(AbstractedLink helper){
        this.helper = helper;
    }

    public abstract void printTesting();

    public abstract Entity createMountableEntity(EntityType entityType, World world);
    public abstract Entity createFollowingEntity(EntityType entityType, World world, Entity leader);

    public abstract void moveEntityTo(Entity entity, Player player);
    public abstract void moveEntityTo(Entity self, Vector to);
    public abstract void setEntityColor(Entity entity, DyeColor color);

}
