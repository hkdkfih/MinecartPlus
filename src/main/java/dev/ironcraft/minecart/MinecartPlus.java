package dev.ironcraft.minecart;

import dev.ironcraft.minecart.command.IronCraftRailCommand;
import dev.ironcraft.minecart.config.RailSettings;
import dev.ironcraft.minecart.item.CustomRailItemFactory;
import dev.ironcraft.minecart.listener.CopperRailLifecycleListener;
import dev.ironcraft.minecart.physics.MinecartPhysicsListener;
import dev.ironcraft.minecart.rail.CustomRailType;
import dev.ironcraft.minecart.storage.CustomRailRegistry;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinecartPlus extends JavaPlugin {
    private RailSettings settings;
    private CustomRailItemFactory items;
    private CustomRailRegistry registry;
    private MinecartPhysicsListener physics;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        migrateConfig();
        try {
            settings = RailSettings.load(getConfig());
        } catch (IllegalArgumentException exception) {
            getLogger().severe("Invalid configuration: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        items = new CustomRailItemFactory(this::settings);
        registry = new CustomRailRegistry();
        physics = new MinecartPhysicsListener(registry, this::settings);

        Bukkit.getPluginManager().registerEvents(new CopperRailLifecycleListener(this, registry, items), this);
        Bukkit.getPluginManager().registerEvents(physics, this);

        IronCraftRailCommand commandHandler = new IronCraftRailCommand(this, items);
        PluginCommand command = Objects.requireNonNull(getCommand("ironcraft"), "ironcraft command missing from plugin.yml");
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        registerRecipes();
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (CustomRailType type : CustomRailType.values()) {
                player.discoverRecipe(type.key());
            }
        }
        physics.initializeLoadedMinecarts();

        getLogger().info(String.format(
                java.util.Locale.ROOT,
                "Enabled for Paper 26.2: powered %.1f, copper %.1f, iron %.1f, diamond %.1f blocks/s.",
                settings.poweredRailSpeed() * 20.0,
                settings.copperPoweredRailSpeed() * 20.0,
                settings.ironPoweredRailSpeed() * 20.0,
                settings.diamondPoweredRailSpeed() * 20.0
        ));
    }

    @Override
    public void onDisable() {
        if (physics != null) {
            physics.shutdown();
        }
        if (registry != null) {
            registry.clearCache();
        }
        for (CustomRailType type : CustomRailType.values()) {
            Bukkit.removeRecipe(type.key());
        }
    }

    public RailSettings settings() {
        return settings;
    }

    public void reloadAddon() {
        reloadConfig();
        migrateConfig();
        RailSettings reloaded = RailSettings.load(getConfig());
        settings = reloaded;
        registerRecipes();
    }

    private void registerRecipes() {
        for (CustomRailType type : CustomRailType.values()) {
            registerRecipe(type);
        }
    }

    private void migrateConfig() {
        if (getConfig().getInt("config-version", 1) >= 2) {
            return;
        }
        getConfig().set("physics.curve-speed", 0.8);
        getConfig().set("config-version", 2);
        saveConfig();
        getLogger().info("Updated classic curve speed to 16 blocks/s.");
    }

    private void registerRecipe(CustomRailType type) {
        NamespacedKey recipeKey = type.key();
        Bukkit.removeRecipe(recipeKey);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, items.create(type, settings.recipeResultAmount()));
        recipe.shape("M M", "MSM", "MRM");
        recipe.setIngredient('M', type.recipeMaterial());
        recipe.setIngredient('S', Material.STICK);
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setCategory(CraftingBookCategory.REDSTONE);
        recipe.setGroup("ironcraft_" + type.path());
        if (!Bukkit.addRecipe(recipe)) {
            getLogger().warning("Could not register " + type.displayName() + "; another recipe may use " + recipeKey + '.');
        }
    }
}
