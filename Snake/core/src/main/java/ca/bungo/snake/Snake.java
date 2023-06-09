package ca.bungo.snake;

import ca.bungo.snake.abstracted.AbstractedHandler;
import ca.bungo.snake.abstracted.AbstractedLink;
import ca.bungo.snake.commands.ArenaCommand;
import ca.bungo.snake.commands.SnakeCommand;
import ca.bungo.snake.commands.TestCommand;
import ca.bungo.snake.managers.ArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;

public final class Snake extends JavaPlugin {

    private static Snake instance;
    public AbstractedHandler abstractedHandler;
    public ArenaManager arenaManager;

    private final String pkg = this.getClass().getCanonicalName().substring(0,
            this.getClass().getCanonicalName().length()-this.getClass().getSimpleName().length());

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        this.saveDefaultConfig();

        loadAbstract();

        registerCommands();

        this.arenaManager = new ArenaManager();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void registerCommands(){
        getServer().getCommandMap().register("snake", new TestCommand("test"));
        getServer().getCommandMap().register("snake", new ArenaCommand("arena"));
        getServer().getCommandMap().register("snake", new SnakeCommand("snake"));
    }

    public static Snake getInstance(){
        return instance;
    }

    private void loadAbstract(){

        AbstractedLink helper =  new AbstractedLink(){
            @Override
            public Plugin getInstance() {
                return Snake.getInstance();
            }
        };

        String ver = Bukkit.getServer().getClass().getPackage().getName().replace('.', ',').split(",")[3];
        getLogger().info("Attempting to load version: " + ver);
        try {
            Class<?> handler = Class.forName(pkg + "abstracted." + ver + ".Abstracted" + ver);
            this.abstractedHandler = (AbstractedHandler) handler.getConstructor(AbstractedLink.class).newInstance(helper);
            getLogger().info("Loaded NMS version: " + ver + "!");
        } catch(ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                InvocationTargetException e){
            e.printStackTrace();
            getLogger().warning("Failed to find Abstract Handlder for version: " + ver);
            getLogger().warning("Attempted Class: " + pkg + "abstracted." + ver + ".Abstracted" + ver);
        }

        abstractedHandler.printTesting();
    }
}
