package ca.bungo.snake.types;

import ca.bungo.snake.Snake;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayerSnake implements Listener {

    private final List<Entity> playerSnake = new ArrayList<>();

    private int maxSnakeSize = 3;
    private boolean pendingSnake = false;

    private final Player owningPlayer;
    private final Location spawnLocation;
    private Arena arena;
    private DyeColor color;

    public PlayerSnake(Player owningPlayer, Location spawnLocation, Arena arena, DyeColor color){
        this.owningPlayer = owningPlayer;
        this.spawnLocation = spawnLocation;
        this.arena = arena;
        this.color = color;

        createSnake();
    }

    private void createSnake(){
        Entity snakeEntity = Snake.getInstance().abstractedHandler.createMountableEntity(EntityType.SHEEP, owningPlayer.getWorld());
        Snake.getInstance().abstractedHandler.setEntityColor(snakeEntity, color);
        snakeEntity.spawnAt(spawnLocation);
        snakeEntity.addPassenger(owningPlayer);
        playerSnake.add(snakeEntity);
    }

    private void growBody(Location location){
        pendingSnake = false;
        if(playerSnake.size() == 0)
            return;
        Entity snakeEntity = playerSnake.get(playerSnake.size()-1);
        Entity extendedBody = Snake.getInstance().abstractedHandler.createFollowingEntity(EntityType.SHEEP, owningPlayer.getWorld(), snakeEntity);
        Snake.getInstance().abstractedHandler.setEntityColor(extendedBody, color);
        extendedBody.spawnAt(location);
        playerSnake.add(extendedBody);
    }

    public void disable(){
        ServerTickEndEvent.getHandlerList().unregister(this);
        EntityDismountEvent.getHandlerList().unregister(this);
        EntityMoveEvent.getHandlerList().unregister(this);

        for(Entity ent : playerSnake){
            ent.getLocation().getWorld().spawnParticle(Particle.CLOUD, ent.getLocation(), 25);
            ent.remove();
        }
        playerSnake.clear();
    }

    public Player getOwningPlayer(){
        return this.owningPlayer;
    }

    public boolean isPartOfSnake(Entity entity){
        return this.playerSnake.contains(entity);
    }

    public boolean isHeadOfSnake(Entity entity){
        return this.playerSnake.get(0).equals(entity);
    }

    public void increaseSize(int size){
        this.maxSnakeSize+= size;
    }

    public int getSize(){
        return this.maxSnakeSize;
    }

    @EventHandler
    public void onTick(ServerTickEndEvent event){
        Snake.getInstance().abstractedHandler.moveEntityTo(playerSnake.get(0), (Player) playerSnake.get(0).getPassengers().get(0));
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event){
        if(!playerSnake.contains(event.getDismounted())) return;
        if(event.getEntity() instanceof Player player){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent event){
        if(!isHeadOfSnake(event.getEntity())) {
            if(isPartOfSnake(event.getEntity())){
                if(playerSnake.get(playerSnake.size()-1).equals(event.getEntity())){
                    if(maxSnakeSize > playerSnake.size() && !pendingSnake){
                        pendingSnake = true;
                        this.growBody(event.getFrom());
                    }
                }
                int idx = playerSnake.indexOf(event.getEntity());
                if(playerSnake.size() > idx+1){
                    Location from = event.getFrom();
                    Location ogLoc = playerSnake.get(idx+1).getLocation();

                    Vector delta = new Vector(from.getX()-ogLoc.getX(), from.getY()-ogLoc.getY(), from.getZ()-ogLoc.getZ());
                    delta.multiply(0.5);

                    Snake.getInstance().abstractedHandler.moveEntityTo(playerSnake.get(idx+1), delta);
                }
            }
            return;
        } else if(playerSnake.size() == 1 && !pendingSnake){
            pendingSnake = true;
            this.growBody(event.getFrom());
        }
        Entity snake = event.getEntity();

        int idx = playerSnake.indexOf(event.getEntity());
        if(playerSnake.size() > idx+1){
            Location from = event.getFrom();
            Location ogLoc = playerSnake.get(idx+1).getLocation();

            Vector delta = new Vector(from.getX()-ogLoc.getX(), from.getY()-ogLoc.getY(), from.getZ()-ogLoc.getZ());
            delta.multiply(0.5);

            playerSnake.get(idx+1).setVelocity(delta);
        }


        List<Entity> nonSelf = new ArrayList<>();
        Collection<Entity> nearMovingTo = event.getTo().getNearbyEntities(0.4, 1, 0.4);
        for(Entity ent : nearMovingTo){
            if(ent == snake) continue;
            if(playerSnake.size() < 3) break;
            if(ent == playerSnake.get(1)) continue;
            nonSelf.add(ent);
        }

        List<Entity> food = new ArrayList<>();
        List<Entity> sheep = new ArrayList<>();
        for(Entity ent : nonSelf){
            if(ent.getType().equals(EntityType.SLIME))
                food.add(ent);
            else if(ent.getType().equals(EntityType.SHEEP) || ent.getType().equals(EntityType.UNKNOWN))
                sheep.add(ent);
        }

        if(sheep.size() > 0){
            arena.hitSheep(this, sheep.get(0)); //Only really care about 1 of the sheep since no others should matter
        }
        else if(food.size() > 0){
            Slime _food = (Slime) food.get(0);
            Location foodLoc = _food.getLocation();
            foodLoc.getWorld().spawnParticle(Particle.CLOUD, foodLoc, 25);
            _food.remove();
            foodLoc.getWorld().playSound(foodLoc, Sound.ENTITY_PLAYER_BURP, 1, 1);
            this.maxSnakeSize+=_food.getSize();
            arena.decreaseSlimeCount(_food);
        }
    }

}
