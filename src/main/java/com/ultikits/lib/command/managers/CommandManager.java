package com.ultikits.lib.command.managers;

import com.ultikits.lib.command.abstracts.AbstractCommendExecutor;
import com.ultikits.lib.command.annotations.CmdExecutor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command manager.
 * <p>
 * 命令管理器
 */
public class CommandManager {
    private final List<Command> commandList = new ArrayList<>();
    @Getter
    private final Plugin plugin;

    /**
     * Constructs a new CommandManager instance.
     *
     * @param plugin The plugin instance.
     */
    public CommandManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Register command.
     * <p>
     * 注册命令
     *
     * @param commandExecutor Command executor <br> 命令执行器
     */
    public void register(AbstractCommendExecutor commandExecutor) {
        commandExecutor.setPlugin(plugin);
        if (!commandExecutor.getClass().isAnnotationPresent(CmdExecutor.class)) {
            plugin.getLogger().warning("Command executor class " + commandExecutor.getClass().getName() + " does not have CmdExecutor annotation.");
            return;
        }
        CmdExecutor annotation = commandExecutor.getClass().getAnnotation(CmdExecutor.class);
        PluginCommand command = register(commandExecutor, annotation.permission(), annotation.description(), annotation.alias());
        commandList.add(command);
    }

    /**
     * Don't use this method to register commands, use {@link #register(AbstractCommendExecutor)} instead.
     * <p>
     * 不要使用此方法注册命令，使用{@link #register(AbstractCommendExecutor)}代替。
     *
     * @param commandExecutor Command executor instance <br> 命令执行器实例
     * @param permission      Permission <br> 权限
     * @param description     Description <br> 描述
     * @param aliases         Aliases <br> 别名
     */
    private PluginCommand register(CommandExecutor commandExecutor, String permission, String description, String... aliases) {
        PluginCommand command = getCommand(aliases[0], plugin);
        command.setAliases(Arrays.asList(aliases));
        command.setPermission(permission);
        command.setDescription(description);
        getCommandMap().register(plugin.getDescription().getName(), command);
        command.setExecutor(commandExecutor);
        return command;
    }

    /**
     * @param name Command name <br> 命令名
     */
    public void unregister(String name) {
        PluginCommand command = getCommand(name, plugin);
        command.unregister(getCommandMap());
    }

    /**
     * Unregister all commands registered by the plugin.
     * <p>
     * 注销插件注册的所有命令
     */
    public void unregisterAll() {
        for (Command command : commandList) {
            unregister(command.getName());
        }
    }

    /**
     * Creates a new PluginCommand instance using reflection.
     *
     * @param name   The name of the command.
     * @param plugin The plugin instance.
     * @return A new PluginCommand instance, or null if an error occurred.
     */
    private PluginCommand getCommand(String name, Plugin plugin) {
        PluginCommand command = null;

        try {
            Constructor<PluginCommand> c = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            c.setAccessible(true);

            command = c.newInstance(name, plugin);
        } catch (Exception | Error e) {
            e.printStackTrace();
        }

        return command;
    }

    /**
     * Retrieves the CommandMap instance using reflection.
     *
     * @return The CommandMap instance, or null if an error occurred.
     */
    private CommandMap getCommandMap() {
        CommandMap commandMap = null;

        try {
            if (Bukkit.getPluginManager() instanceof SimplePluginManager) {
                Field f = SimplePluginManager.class.getDeclaredField("commandMap");
                f.setAccessible(true);

                commandMap = (CommandMap) f.get(Bukkit.getPluginManager());
            }
        } catch (Exception | Error e) {
            e.printStackTrace();
        }

        return commandMap;
    }

}
