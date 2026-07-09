package io.github.jeremyjenki.playerheight;

import org.bukkit.plugin.java.JavaPlugin;

public class PlayerHeight extends JavaPlugin {

    private DatabaseManager database;
    private AttributeConfig attributeConfig;
    private AttributeApplier applier;
    private FeedbackManager feedback;
    private MessageManager messages;
    private WorldConfig worldConfig;
    private PotionFactory potionFactory;
    private PlayerListener playerListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        attributeConfig = new AttributeConfig(getLogger());
        attributeConfig.load(new java.io.File(getDataFolder(), "config.yml"), getConfig());

        database = new DatabaseManager(this);
        database.connect();

        applier = new AttributeApplier(attributeConfig, getLogger());

        feedback = new FeedbackManager();
        feedback.load(getConfig());

        worldConfig = new WorldConfig(getLogger());
        worldConfig.load(getConfig());

        messages = new MessageManager(this);
        messages.load(getConfig().getString("language", "en_US"));

        playerListener = new PlayerListener(this);
        potionFactory = new PotionFactory(playerListener.getScaleDeltaKey(), feedback, this);

        getServer().getPluginManager().registerEvents(playerListener, this);

        AdminCommand adminCommand = new AdminCommand(this);
        ScaleCommand scaleCommand = new ScaleCommand(this);
        adminCommand.setScaleCommand(scaleCommand);

        getCommand("playerheight").setExecutor(adminCommand);
        getCommand("playerheight").setTabCompleter(adminCommand);
        getCommand("scale").setExecutor(scaleCommand);
        getCommand("scale").setTabCompleter(scaleCommand);

        getCommand("setscale").setExecutor((sender, command, label, args) ->
                adminCommand.onCommand(sender, command, label, prependSetscale(args)));
        getCommand("setscale").setTabCompleter((sender, command, label, args) ->
                adminCommand.onTabComplete(sender, command, label, prependSetscale(args)));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlayerHeightExpansion(this).register();
            getLogger().info("PlaceholderAPI found — placeholders registered.");
        }

        getLogger().info("PlayerHeight enabled. PDC key: " + playerListener.getScaleDeltaKey());
    }

    @Override
    public void onDisable() {
        if (database != null) database.disconnect();
        getLogger().info("PlayerHeight disabled.");
    }

    public DatabaseManager getDatabase()        { return database; }
    public AttributeConfig getAttributeConfig() { return attributeConfig; }
    public AttributeApplier getApplier()        { return applier; }
    public FeedbackManager getFeedback()        { return feedback; }
    public MessageManager getMessages()         { return messages; }
    public WorldConfig getWorldConfig()         { return worldConfig; }
    public PotionFactory getPotionFactory()     { return potionFactory; }
    public PlayerListener getPlayerListener()   { return playerListener; }

    private static String[] prependSetscale(String[] args) {
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = "setscale";
        System.arraycopy(args, 0, newArgs, 1, args.length);
        return newArgs;
    }
}
