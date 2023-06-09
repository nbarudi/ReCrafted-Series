package ca.bungo.snake.types;

import ca.bungo.snake.Snake;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Arena {

    public enum StartReason {
        FORCED,AUTO
    }

    private FileConfiguration config;
    private ConfigurationSection arenaCfg;

    private String name;
    private World world;
    private CuboidRegion region;

    private Component messagePrefix;


    private List<String> activePlayers = new ArrayList<>();
    private List<PlayerSnake> snakes = new ArrayList<>();
    private List<String> alivePlayers = new ArrayList<>();
    private List<Entity> slimes = new ArrayList<>();
    private List<DyeColor> options = new ArrayList<>();

    private int slimeCount = 0;

    /**
     * @param arenaCfg The arena configuration section containing World, StartPos, and EndPos
     * */
    public Arena(@NotNull ConfigurationSection arenaCfg){
        this.config = Snake.getInstance().getConfig();
        this.arenaCfg = arenaCfg;
        this.name = arenaCfg.getName();
        World _world = Bukkit.getWorld(arenaCfg.getString("world"));;
        if(_world == null){
            Snake.getInstance().getLogger().severe("World for arena " + this.name + " does not exist!");
            return;
        }
        if(arenaCfg.getConfigurationSection("spawn") == null){
            Snake.getInstance().getLogger().severe("Spawn point for arena " + this.name + " does not exist!");
        }
        this.world =_world;

        BlockVector3 startPos = BlockVector3.at(arenaCfg.getInt("startPos.x"),
                                arenaCfg.getInt("startPos.y"),
                                arenaCfg.getInt("startPos.z"));
        BlockVector3 endPos = BlockVector3.at(arenaCfg.getInt("endPos.x"),
                arenaCfg.getInt("endPos.y"),
                arenaCfg.getInt("endPos.z"));

        this.region = new CuboidRegion(BukkitAdapter.adapt(world), startPos, endPos);

        messagePrefix = Component.text("[", NamedTextColor.GRAY)
                .append(Component.text(this.name, NamedTextColor.AQUA))
                .append(Component.text("] ", NamedTextColor.GRAY));
    }


    public String getName(){
        return this.name;
    }

    public List<String> getActivePlayers(){
        return this.activePlayers;
    }

    public void connectPlayer(Player player){
        this.activePlayers.add(player.getUniqueId().toString());
        broadcastPlayers(player.getName() + " has joined!");
        World world = Bukkit.getWorld(arenaCfg.getString("spawn.world"));
        if(world == null){
            Snake.getInstance().getLogger().severe("World for arena " + this.name + " does not exist!");
            return;
        }
        Location location = new Location(world, arenaCfg.getInt("spawn.x"), arenaCfg.getInt("spawn.y"), arenaCfg.getInt("spawn.z"));
        player.teleport(location);
        tryAutoStart();
    }

    public void disconnectPlayer(Player player){
        this.activePlayers.remove(player.getUniqueId().toString());
        World world = Bukkit.getWorld(config.getString("spawn.world"));
        Location spawn = new Location(world, config.getInt("spawn.x"), config.getInt("spawn.y"), config.getInt("spawn.z"));
        player.teleport(spawn);

        for(Player _plr : Bukkit.getOnlinePlayers()){
            if(_plr == player) continue;
            _plr.showPlayer(Snake.getInstance(), player);
        }

        broadcastPlayers(player.getName() + " has left!");
    }

    public void hitSheep(PlayerSnake snake, Entity hit){
        for(PlayerSnake _snake : snakes){
            if(_snake.isPartOfSnake(hit)){
                snake.disable();
                //_snake.getOwningPlayer().getName() + " got in the way of " + snake.getOwningPlayer().getName() + " and took them out!"
                broadcastPlayers(Component.text(_snake.getOwningPlayer().getName(), NamedTextColor.AQUA)
                        .append(Component.text(" got in the way of ", NamedTextColor.YELLOW))
                        .append(Component.text(snake.getOwningPlayer().getName(), NamedTextColor.RED))
                        .append(Component.text(" and took them out!", NamedTextColor.YELLOW)));
                alivePlayers.remove(snake.getOwningPlayer().getUniqueId().toString());
                snake.getOwningPlayer().setAllowFlight(true);
                snake.getOwningPlayer().setFlying(true);
                for(Player _p : Bukkit.getOnlinePlayers()){
                    if(_p == snake.getOwningPlayer()) continue;
                    _p.hidePlayer(Snake.getInstance(), snake.getOwningPlayer());
                }
                playSoundPlayers(Sound.ENTITY_PLAYER_HURT);
                _snake.increaseSize(snake.getSize()/2);
            }
        }
    }

    private void tryAutoStart(){
        if(this.activePlayers.size() >= 8){
            startGame(StartReason.AUTO);
        }
    }

    public void stopGame(StartReason stopReason){
        if(stopReason.equals(StartReason.FORCED)){
            broadcastPlayers("The game has been force ended!");
            trueStop();
        }
    }

    public void startGame(StartReason startReason){
        options.clear();
        options.addAll(List.of(DyeColor.values()));
        if(startReason.equals(StartReason.FORCED)){
            broadcastPlayers("The game has been force started!");
            trueStart();
        }
        else if(startReason.equals(StartReason.AUTO)){
            AtomicInteger counter = new AtomicInteger(10); //interesting
            broadcastPlayers("The game will begin in " + counter.getAndDecrement() + " seconds!");

            new BukkitRunnable(){

                @Override
                public void run() {
                    broadcastPlayers("The game will begin in " + counter.getAndDecrement() + " seconds!");

                    playSoundPlayers(Sound.BLOCK_NOTE_BLOCK_PLING);

                    if(counter.get() < 0){
                        trueStart();
                        playSoundPlayers(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
                        cancel();
                    }
                }
            }.runTaskTimer(Snake.getInstance(), 20, 20);

        }
    }

    private void trueStop(){
        for(PlayerSnake snake : snakes){
            snake.disable();
        }
        for(Entity slime : slimes){
            slime.remove();
        }
        for(String uuid : activePlayers){
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            if(player == null) continue;

            for(Player _plr : Bukkit.getOnlinePlayers()){
                if(_plr == player) continue;
                _plr.showPlayer(Snake.getInstance(), player);
            }
        }
        activePlayers.clear();
        alivePlayers.clear();
        slimes.clear();
        slimeCount = 0;
        snakes.clear();
    }

    private void trueStart(){
        int deltaX = Math.abs(region.getPos1().getX() - region.getPos2().getX());
        int deltaZ = Math.abs(region.getPos1().getZ() - region.getPos2().getZ());

        int dist = Math.floorDiv((int)Math.floor(Math.sqrt(Math.pow(deltaZ, 2) + Math.pow(deltaX, 2))), 4);

        Vector3 centerPoint = region.getCenter();
        BlockVector3 center = BlockVector3.at(centerPoint.getX(), arenaCfg.getInt("spawn.y"), centerPoint.getZ());

        if(activePlayers.size() == 0)
            return;

        List<BlockVector3> spawnPoints = new ArrayList<>();

        for(int i = 0; i < activePlayers.size(); i++){
            double x = center.getX() + dist * Math.cos(2*Math.PI*i/activePlayers.size());
            double z = center.getZ() + dist + Math.sin(2*Math.PI*i/activePlayers.size());

            spawnPoints.add(BlockVector3.at(x, center.getY(), z));
        }

        Random rnd = new Random();

        for(String uuid : activePlayers){
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            if(player == null) continue;
            int index = rnd.nextInt(0, spawnPoints.size());
            BlockVector3 spawnPoint = spawnPoints.remove(index);
            int colorIndex = rnd.nextInt(0, options.size());
            DyeColor color = options.remove(colorIndex);

            snakes.add(new PlayerSnake(player, new Location(world, spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ()), this, color));
        }

        for(PlayerSnake snake : snakes){
            Snake.getInstance().getServer().getPluginManager().registerEvents(snake, Snake.getInstance());
        }

        alivePlayers.addAll(activePlayers);

        startGameLoop();

    }

    private void startGameLoop(){

        Random rnd = new Random();
        List<Location> possibleSlimeSpawns = new ArrayList<>();
        for(BlockVector2 zone : region.asFlatRegion()){
            Location posLoc = new Location(world, zone.getX(), arenaCfg.getInt("spawn.y"), zone.getZ());
            if(posLoc.getBlock().getType() != Material.AIR) continue;
            possibleSlimeSpawns.add(posLoc);
        }

        new BukkitRunnable(){
            public void run(){
                if(alivePlayers.size() == 0){
                    broadcastPlayers("The game is over! The winner is:");
                    if(alivePlayers.size() != 0)
                        broadcastPlayers(Component.text(Bukkit.getPlayer(UUID.fromString(alivePlayers.get(0))).getName(), NamedTextColor.GREEN));
                    else
                        broadcastPlayers("No One!");
                    trueStop();
                    cancel(); //Game is Over!
                }else{
                    while (slimeCount < alivePlayers.size()+2){
                        int idx = rnd.nextInt(0, possibleSlimeSpawns.size());
                        Slime slime = (Slime) world.spawnEntity(possibleSlimeSpawns.get(idx), EntityType.SLIME);
                        slime.setInvulnerable(true);
                        slime.setAI(false);
                        slime.setSize(rnd.nextInt(2, 5));
                        slimes.add(slime);
                        slimeCount++;
                    }
                }
            }
        }.runTaskTimer(Snake.getInstance(), 1, 1);
    }


    public void decreaseSlimeCount(Entity slime){
        slimes.remove(slime);
        this.slimeCount--;
    }


    private void broadcastPlayers(String message){
        for(String uuid : activePlayers){
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            if(player != null){
                player.sendMessage(messagePrefix.append(Component.text(message, NamedTextColor.YELLOW)));
            }
        }
    }
    private void broadcastPlayers(Component message){
        for(String uuid : activePlayers){
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            if(player != null){
                player.sendMessage(messagePrefix.append(message));
            }
        }
    }

    private void playSoundPlayers(Sound sound){
        for(String uuid : activePlayers){
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            if(player != null){
                player.playSound(player.getLocation(), sound, 1, 1);
            }
        }
    }
}
