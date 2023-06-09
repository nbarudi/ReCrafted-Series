package ca.bungo.snake.managers;

import ca.bungo.snake.Snake;
import ca.bungo.snake.types.Arena;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ArenaManager {

    private Map<String, Arena> arenaList;
    private Map<String, Arena> playerConnection;
    private ConfigurationSection arenasSection;

    public ArenaManager(){
        FileConfiguration config = Snake.getInstance().getConfig();
        if(config.getConfigurationSection("arenas") == null)
            return;
        this.arenasSection = config.getConfigurationSection("arenas");
        arenaList = new HashMap<>();
        playerConnection = new HashMap<>();

        for(String key : arenasSection.getKeys(false)){
            ConfigurationSection arenaCfg = arenasSection.getConfigurationSection(key);
            Arena arena = new Arena(arenaCfg);
            arenaList.put(key, arena);
        }
    }

    public void reloadArenas(){
        arenaList.clear();
        for(String key : arenasSection.getKeys(false)){
            ConfigurationSection arenaCfg = arenasSection.getConfigurationSection(key);
            Arena arena = new Arena(arenaCfg);
            arenaList.put(key, arena);
        }
    }

    public Arena getArena(String name){
        if(!arenaList.containsKey(name))
            return null;
        return arenaList.get(name);
    }

    public boolean joinArena(Player player, String arenaName){
        Arena arena = getArena(arenaName);
        if(arena == null){
            return false;
        }
        playerConnection.put(player.getUniqueId().toString(), arena);
        arena.connectPlayer(player);
        return true;
    }

    public boolean leaveArena(Player player){
        Arena arena = playerConnection.remove(player.getUniqueId().toString());
        if(arena != null){
            arena.disconnectPlayer(player);
            return true;
        }
        return false;
    }

    public void forceStart(String arenaName){
        Arena arena = arenaList.get(arenaName);
        if(arena == null)
            return;
        arena.startGame(Arena.StartReason.AUTO);
    }

    public void forceStop(String arenaName){
        Arena arena = arenaList.get(arenaName);
        if(arena == null)
            return;
        arena.stopGame(Arena.StartReason.FORCED);
    }

}
