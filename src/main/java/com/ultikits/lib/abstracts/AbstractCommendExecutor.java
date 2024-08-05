package com.ultikits.lib.abstracts;

import cn.hutool.core.util.ReflectUtil;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ultikits.lib.annotations.*;
import com.ultikits.lib.common.CheckResponse;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Function;

/**
 * This abstract class represents a command executor.
 * It implements the TabExecutor interface from the Bukkit API.
 * <p>
 * 这个抽象类代表了一个命令执行器。
 * 它实现了Bukkit API中的TabExecutor接口。
 *
 * @see TabExecutor
 */
public abstract class AbstractCommendExecutor implements TabExecutor {
    private final BiMap<String, Method> mappings = HashBiMap.create();
    private final BiMap<UUID, Method> senderLock = HashBiMap.create();
    private final BiMap<UUID, Method> serverLock = HashBiMap.create();
    private final BiMap<UUID, Method> cmdCoolDown = HashBiMap.create();
    @Getter
    private final Map<List<Class<?>>, Function<String, ?>> parsers = new HashMap<>();
    @Setter
    private Plugin plugin;

    /**
     * Constructor that initializes parsers and scans command mappings.
     * <p>
     * 构造函数，初始化解析器并扫描命令映射。
     */
    public AbstractCommendExecutor() {
        initParsers();
        scanCommandMappings();
    }

    /**
     * Checks whether the sender is valid.
     * <p>
     * 检查发送者是否有效。
     *
     * @param sender    The sender of the command. <br> 命令的发送者。
     * @param cmdTarget The method that matches the command. <br> 匹配命令的方法。
     * @return Whether the sender is valid. <br> 发送者是否有效。
     */
    private CheckResponse checkCmdTargetType(CommandSender sender, CmdTarget cmdTarget) {
        if (cmdTarget.value().equals(CmdTarget.CmdTargetType.PLAYER) && !(sender instanceof Player)) {
            return new CheckResponse("This command can only be executed by a player.");
        }
        if (!cmdTarget.value().equals(CmdTarget.CmdTargetType.CONSOLE) || !(sender instanceof Player))
            return CheckResponse.SUCCESS;
        return new CheckResponse("This command can only be executed by the console.");
    }

    /**
     * Gets a map of parameter match from a command.
     * <p>
     * 从命令中获取参数匹配的映射。
     *
     * @param args   The arguments of the command. <br> 命令的参数。
     * @param format The format of the command. <br> 命令的格式。
     * @return The map of the parameters. <br> 参数的映射。
     */
    private Map<String, String[]> getParams(String[] args, String format) {
        if (args.length == 0) {
            return Collections.emptyMap();
        }

        String[] formatArgs = format.split(" ");
        Map<String, String[]> params = new HashMap<>();
        List<String> paramList = new ArrayList<>();
        int index = 0;
        for (String arg : args) {
            String currentFormatArg = formatArgs[index];
            if (currentFormatArg.startsWith("<")) {
                String paramName = currentFormatArg.substring(1, currentFormatArg.length() - (currentFormatArg.endsWith("...>") ? 4 : 1));
                if (currentFormatArg.endsWith("...>")) {
                    paramList.add(arg);
                } else {
                    params.put(paramName, new String[]{arg});
                }
            }
            index++;
        }

        if (!paramList.isEmpty()) {
            params.put(formatArgs[index].substring(1, formatArgs[index].length() - 1), paramList.toArray(new String[0]));
        }

        return params;
    }

    /**
     * Gets the instance of the command executor.
     * <p>
     * 获取命令执行器的实例。
     *
     * @return The instance of the command executor. <br> 命令执行器的实例。
     */
    public AbstractCommendExecutor getInstance() {
        return this;
    }

    /**
     * Initializes the parsers.
     * <p>
     * 初始化解析器。
     */
    @SuppressWarnings("deprecation")
    private void initParsers() {
        parsers.put(Arrays.asList(Boolean[].class, Boolean.class, boolean[].class, boolean.class), Boolean::parseBoolean);
        parsers.put(Arrays.asList(Double[].class, Double.class, double[].class, double.class), Double::parseDouble);
        parsers.put(Arrays.asList(Integer[].class, Integer.class, int[].class, int.class), Integer::parseInt);
        parsers.put(Arrays.asList(Float[].class, Float.class, float[].class, float.class), Float::parseFloat);
        parsers.put(Arrays.asList(Short[].class, Short.class, short[].class, short.class), Short::parseShort);
        parsers.put(Arrays.asList(Short[].class, Short.class, short[].class, short.class), Byte::parseByte);
        parsers.put(Arrays.asList(OfflinePlayer[].class, OfflinePlayer.class), Bukkit::getOfflinePlayer);
        parsers.put(Arrays.asList(Long[].class, Long.class, long[].class, long.class), Long::parseLong);
        parsers.put(Arrays.asList(Material[].class, Material.class), Material::getMaterial);
        parsers.put(Arrays.asList(Player[].class, Player.class), Bukkit::getPlayerExact);
        parsers.put(Arrays.asList(UUID[].class, UUID.class), UUID::fromString);
        parsers.put(Arrays.asList(String[].class, String.class), s -> s);
    }

    /**
     * Gets the parser of the parameter.
     * <p>
     * 获取参数的解析器。
     *
     * @param type The type of the parameter. <br> 参数的类型。
     * @param <T>  The type of the parameter. <br> 参数的类型。
     * @return The parser of the parameter. <br> 参数的解析器。
     */
    private <T> Function<String, T> getParser(Class<T> type) {
        //noinspection unchecked
        return (Function<String, T>) parsers.keySet().stream()
                .filter(classes -> classes.stream().anyMatch(clazz -> clazz.isAssignableFrom(type)))
                .findFirst()
                .map(parsers::get)
                .orElse(null);
    }

    /**
     * Scans the command mappings.
     * <p>
     * 扫描命令映射。
     */
    private void scanCommandMappings() {
        Class<? extends AbstractCommendExecutor> clazz = this.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(CmdMapping.class)) {
                mappings.put(method.getAnnotation(CmdMapping.class).format(), method);
            }
        }
    }

    /**
     * Matches the command.
     * <p>
     * 匹配命令。
     *
     * @param args The arguments of the command. <br> 命令的参数。
     * @return The method that matches the command. <br> 匹配命令的方法。
     */
    private Method matchMethod(String[] args) {
        if (args.length == 0) {
            return mappings.getOrDefault("", null);
        }
        for (Map.Entry<String, Method> entry : mappings.entrySet()) {
            String format = entry.getKey();
            String[] formatArgs = format.split(" ");

            boolean match = true;

            // 检查参数长度是否一致
            if (formatArgs.length != args.length) {
                // 参数长度不一致 取可配对参数的最小值
                int min = Math.min(formatArgs.length, args.length);

                // 逐个匹配
                for (int i = 0; i < min; i++) {
                    if (!matchesArgument(formatArgs[i], args[i])) {
                        match = false;
                        break;
                    }
                }

                // 如果所有参数都匹配，返回这个方法
                if (match) {
                    return entry.getValue();
                }

                // 如果不完全匹配，继续下一次循环
                continue;
            }

            for (int i = 0; i < formatArgs.length - 1; i++) {
                if (!matchesArgument(formatArgs[i], args[i])) {
                    match = false;
                    break;
                }
            }

            if (match && matchesLastArgument(formatArgs[formatArgs.length - 1], args[formatArgs.length - 1])) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Compare the actual argument with the format argument.
     * <p>
     * 将实际参数与格式参数进行比较。
     *
     * @param formatArg The format argument. <br> 格式参数。
     * @param actualArg The actual argument. <br> 实际参数。
     * @return Whether the actual argument matches the format argument. <br> 实际参数是否与格式参数匹配。
     */
    private boolean matchesArgument(String formatArg, String actualArg) {
        return formatArg.startsWith("<") && formatArg.endsWith(">") || formatArg.equalsIgnoreCase(actualArg);
    }

    /**
     * Compare the actual argument with the format argument.
     * <p>
     * 将实际参数与格式参数进行比较。
     *
     * @param formatArg The format argument. <br> 格式参数。
     * @param actualArg The actual argument. <br> 实际参数。
     * @return Whether the actual argument matches the format argument. <br> 实际参数是否与格式参数匹配。
     */
    private boolean matchesLastArgument(String formatArg, String actualArg) {
        if (formatArg.endsWith("...>")) {
            return true;
        }
        return matchesArgument(formatArg, actualArg);
    }

    /**
     * Checks whether the sender is valid.
     * <p>
     * 检查发送者是否有效。
     *
     * @param sender The sender of the command. <br> 命令的发送者。
     * @param method The method that matches the command. <br> 匹配命令的方法。
     * @return Whether the sender is valid. <br> 发送者是否有效。
     */
    private CheckResponse checkSender(CommandSender sender, Method method) {
        if (!method.isAnnotationPresent(CmdTarget.class)) {
            Class<? extends AbstractCommendExecutor> clazz = this.getClass();
            if (!clazz.isAnnotationPresent(CmdTarget.class)) {
                return CheckResponse.SUCCESS;
            }
            CmdTarget cmdTarget = clazz.getAnnotation(CmdTarget.class);
            return checkCmdTargetType(sender, cmdTarget);
        }
        CmdTarget cmdTarget = method.getAnnotation(CmdTarget.class);
        return checkCmdTargetType(sender, cmdTarget);
    }


    /**
     * Checks whether the sender has permission.
     * <p>
     * 检查发送者是否有权限。
     *
     * @param sender The sender of the command. <br> 命令的发送者。
     * @param method The method that matches the command. <br> 匹配命令的方法。
     * @return Whether the sender has permission. <br> 发送者是否有权限。
     */
    private CheckResponse checkPermission(CommandSender sender, Method method) {
        if (!method.isAnnotationPresent(CmdMapping.class)) {
            Class<? extends AbstractCommendExecutor> clazz = this.getClass();
            if (!clazz.isAnnotationPresent(CmdExecutor.class)) {
                return CheckResponse.SUCCESS;
            }
            CmdExecutor cmdExecutor = clazz.getAnnotation(CmdExecutor.class);
            String permission = cmdExecutor.permission();
            if (sender.hasPermission(permission))
                return CheckResponse.SUCCESS;
            return new CheckResponse(String.format("§7Executing this command requires §f%s §7permission", permission));
        }
        CmdMapping cmdMapping = method.getAnnotation(CmdMapping.class);
        if (cmdMapping.permission().isEmpty()) {
            return CheckResponse.SUCCESS;
        }
        String permission = cmdMapping.permission();
        if (sender.hasPermission(permission))
            return CheckResponse.SUCCESS;
        return new CheckResponse(String.format("§7Executing this command requires §f%s §7permission", permission));
    }

    /**
     * Checks whether the sender need to be an OP.
     * <p>
     * 检查发送者是否需要是OP。
     *
     * @param sender The sender of the command. <br> 命令的发送者。
     * @param method The method that matches the command. <br> 匹配命令的方法。
     * @return Whether the sender need to be an OP. <br> 发送者是否需要是OP。
     */
    private CheckResponse checkOp(CommandSender sender, Method method) {
        if (!method.isAnnotationPresent(CmdMapping.class)) {
            Class<? extends AbstractCommendExecutor> clazz = this.getClass();
            if (!clazz.isAnnotationPresent(CmdExecutor.class)) {
                return CheckResponse.SUCCESS;
            }
            CmdExecutor cmdExecutor = clazz.getAnnotation(CmdExecutor.class);
            if (cmdExecutor.requireOp() && sender.isOp())
                return CheckResponse.SUCCESS;
            return new CheckResponse("You do not have permission to execute this command.");
        }
        CmdMapping cmdMapping = method.getAnnotation(CmdMapping.class);
        if (cmdMapping.requireOp() && sender.isOp())
            return CheckResponse.SUCCESS;
        return new CheckResponse("You do not have permission to execute this command.");
    }

    /**
     * Checks whether the sender need to wait for the previous command to finish.
     * <p>
     * 检查发送者是否需要等待上一条命令执行完毕。
     *
     * @param sender The sender of the command. <br> 命令的发送者。
     * @param method The method that matches the command. <br> 匹配命令的方法。
     * @return Whether the sender need to wait for the previous command to finish. <br> 发送者是否需要等待上一条命令执行完毕。
     */
    private CheckResponse checkNotLock(CommandSender sender, Method method) {
        if (!method.isAnnotationPresent(UsageLimit.class)) {
            return CheckResponse.SUCCESS;
        }
        if (method.getAnnotation(UsageLimit.class).value().equals(UsageLimit.LimitType.SENDER)) {
            if (!(sender instanceof Player || method.getAnnotation(UsageLimit.class).ContainConsole())) {
                return CheckResponse.SUCCESS;
            }
            if (sender instanceof Player && senderLock.get(((Player) sender).getUniqueId()).equals(method)) {
                return new CheckResponse("Please wait for the previous command to finish executing first!");
            }
        }
        if (method.getAnnotation(UsageLimit.class).value().equals(UsageLimit.LimitType.ALL)) {
            if (!(sender instanceof Player || method.getAnnotation(UsageLimit.class).ContainConsole())) {
                return CheckResponse.SUCCESS;
            }
            if (sender instanceof Player && serverLock.get(((Player) sender).getUniqueId()).equals(method)) {
                return new CheckResponse("Please wait for the other players' commands to be executed first!");
            }
        }
        return CheckResponse.SUCCESS;
    }

    /**
     * Checks whether the sender need to wait for the command cool down.
     * <p>
     * 检查发送者是否需要等待命令冷却。
     *
     * @param sender The sender of the command. <br> 命令的发送者。
     * @return Whether the sender need to wait for command cool down. <br> 发送者是否需要等待命令冷却。
     */
    private CheckResponse checkNotInCD(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return CheckResponse.SUCCESS;
        }
        if (cmdCoolDown.containsKey(player.getUniqueId()))
            return new CheckResponse("Too many operations. Please try again later.");
        return CheckResponse.SUCCESS;
    }

    /**
     * Builds the parameters of the command.
     * <p>
     * 构建命令的参数。
     *
     * @param strings       The arguments of the command. <br> 命令的参数。
     * @param method        The method that matches the command. <br> 匹配命令的方法。
     * @param commandSender The sender of the command. <br> 命令的发送者。
     * @return Parameters of the command. <br> 命令的参数。
     */
    private Object[] buildParams(String[] strings, Method method, CommandSender commandSender) {
        Map<String, String[]> params = getParams(strings, mappings.inverse().get(method));
        Parameter[] parameters = method.getParameters();

        if (parameters.length == 0) {
            return new Object[0];
        }

        List<Object> paramList = new ArrayList<>();

        for (Parameter parameter : parameters) {
            Class<?> paramType = parameter.getType();

            if (paramType.equals(Player.class) || paramType.equals(CommandSender.class)) {
                boolean isCmdSenderAnnotationPresent = parameter.isAnnotationPresent(CmdSender.class);

                if (paramType.equals(Player.class) && commandSender instanceof Player) {
                    paramList.add(isCmdSenderAnnotationPresent ? commandSender : null);
                } else if (paramType.equals(CommandSender.class)) {
                    paramList.add(isCmdSenderAnnotationPresent ? commandSender : null);
                }

                continue;
            }

            if (parameter.isAnnotationPresent(CmdParam.class)) {
                CmdParam cmdParam = parameter.getAnnotation(CmdParam.class);
                String[] value = params.get(cmdParam.value());
                try {
                    paramList.add(parseType(value, paramType));
                } catch (Exception | Error e) {
                    commandSender.sendMessage(ChatColor.RED + e.getMessage());
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                    return null;
                }
            } else {
                paramList.add(null);
            }
        }
        return paramList.toArray();
    }

    /**
     * Parses the type of the parameter.
     * <p>
     * 解析参数的类型。
     *
     * @param value The value of the parameter. <br> 参数的值。
     * @param type  The type of the parameter. <br> 参数的类型。
     * @param <T>   The type of the parameter. <br> 参数的类型。
     * @return The parsed parameter. <br> 解析后的参数。
     */
    private <T> Object parseType(String[] value, Class<T> type) {
        Function<String, T> parser = getParser(type);
        if (type.isArray()) {
            Object array = Array.newInstance(type.getComponentType(), value.length);
            for (int i = 0; i < value.length; i++) {
                Array.set(array, i, parser.apply(value[i]));
            }
            return array;
        } else {
            return parser.apply(value[0]);
        }
    }

    /**
     * Sets the cool down of the command.
     * <p>
     * 设置命令的冷却。
     *
     * @param commandSender The sender of the command. <br> 命令的发送者。
     * @param method        The method that matches the command. <br> 匹配命令的方法。
     */
    private void setCoolDown(CommandSender commandSender, Method method) {
        if (!(commandSender instanceof Player player)) {
            return;
        }
        CmdCD cmdCD = method.getAnnotation(CmdCD.class);
        if (cmdCD == null) {
            return;
        }
        if (cmdCD.value() == 0) {
            return;
        }
        cmdCoolDown.put(player.getUniqueId(), method);
        new BukkitRunnable() {
            int time = cmdCD.value();

            @Override
            public void run() {
                if (time > 0) {
                    time--;
                } else {
                    cmdCoolDown.remove(player.getUniqueId(), method);
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L);
    }


    /**
     * Abstract method that handles the help command.
     * <p>
     * 处理帮助命令的抽象方法。
     *
     * @param sender The sender of the command. <br> 命令的发送者。
     */
    abstract protected void handleHelp(CommandSender sender);

    /**
     * Tab complete method. Returns a list of possible completions for the specified command string.
     * By rewriting this method, you can customize the tab completion of the command.
     * <p>
     * 补全方法。返回指定命令字符串的可能补全列表。
     * 通过重写此方法，您可以自定义命令的补全。
     *
     * @param player  The player who will see the suggestions. <br> 看到补全的玩家
     * @param command The command that was typed in. <br> 需要补全的命令
     * @param strings The arguments of the command that was typed in. <br> 目前输入的命令参数
     * @return The suggestions. <br> 补全的建议
     */
    protected List<String> suggest(Player player, Command command, String[] strings) {
        List<String> completions = new ArrayList<>();
        if (strings.length == 1) {
            for (Map.Entry<String, Method> entry : mappings.entrySet()) {
                Method method = entry.getValue();
                String format = entry.getKey();
                if (!checkPermission(player, method).isSuccess() || !checkOp(player, method).isSuccess()) {
                    continue;
                }
                String arg = format.split(" ")[0];
                if (arg.startsWith("<") && arg.endsWith(">")) {
                    getArgSuggestion(player, command, strings, method, arg, completions);
                    continue;
                }
                completions.add(arg);
            }
            return completions;
        } else {
            List<Method> methodsByArg = getMethodsByArg(player, String.join(" ", strings));
            for (Method method : methodsByArg) {
                String formatByMethod = getFormatByMethod(method);
                String arg = getArgAt(formatByMethod, strings.length - 1);
                if (arg.startsWith("<") && arg.endsWith(">")) {
                    getArgSuggestion(player, command, strings, method, arg, completions);
                } else {
                    for (String format : mappings.keySet()) {
                        String[] args = format.split(" ");
                        if (args.length < strings.length) {
                            continue;
                        }
                        String sug = args[strings.length - 1];
                        if (sug.startsWith("<") && sug.endsWith(">")) {
                            continue;
                        }
                        completions.add(sug);
                    }
                }
            }
        }
        return completions;
    }

    /**
     * Adds suggestion to the list of suggestions. This method is called by the suggest method.
     * Fetch the suggestions by invoking the given method.
     * <p>
     * 将建议添加到建议列表中。此方法由suggest方法调用。
     * 通过调用给定的方法获取建议。
     *
     * @param player      The player who will see the suggestions. <br> 看到补全的玩家
     * @param command     The command that was typed in. <br> 需要补全的命令
     * @param strings     The arguments of the command that was typed in. <br> 目前输入的命令参数
     * @param method      The method that matches the command. <br> 匹配命令的方法。
     * @param arg         The argument that needs to be completed. <br> 需要补全的参数。
     * @param completions The suggestions. <br> 补全的建议
     */
    private void getArgSuggestion(Player player, Command command, String[] strings, Method method, String arg, List<String> completions) {
        String suggestName = getSuggestName(method, arg.substring(1, arg.length() - 1));
        if (suggestName == null) {
            return;
        }
        Method[] suggestMethod = getSuggestMethodByName(suggestName);
        if (suggestMethod == null || suggestMethod.length == 0) {
            completions.add(suggestName);
            return;
        }
        Class<?> declaringClass = suggestMethod[0].getDeclaringClass();
        Collection<?> suggestObject;
        if (this.getClass() != declaringClass) {
            Object suggestInstance = ReflectUtil.newInstance(declaringClass);
            suggestObject = invokeSuggestMethod(suggestInstance, suggestMethod[0], player, command, strings);
        } else {
            suggestObject = invokeSuggestMethod(this, suggestMethod[0], player, command, strings);
        }
        if (suggestObject != null) {
            for (Object o : suggestObject) {
                completions.add(o.toString());
            }
        }
    }

    /**
     * Gets the format of the command.
     * <p>
     * 获取命令的格式。
     *
     * @param method The method that matches the command. <br> 匹配命令的方法。
     * @return The format of the command. <br> 命令的格式。
     */
    private String getFormatByMethod(Method method) {
        return mappings.inverse().get(method);
    }

    /**
     * Gets the argument at the specified index.
     * <p>
     * 获取指定索引处的参数。
     *
     * @param format The format of the command. <br> 命令的格式。
     * @param index  The index of the argument. <br> 参数的索引。
     * @return The argument at the specified index. <br> 指定索引处的参数。
     */
    private String getArgAt(String format, int index) {
        String[] args = format.split(" ");
        if (args.length <= index) {
            return "";
        }
        return args[index];
    }

    /**
     * Gets the suggest method name of the parameter.
     * <p>
     * 获取参数的建议方法名称。
     *
     * @param method    The method that matches the command. <br> 匹配命令的方法。
     * @param paramName The name of the parameter. <br> 参数的名称。
     * @return The suggest method name of the parameter. <br> 参数的建议方法名称。
     */
    private String getSuggestName(Method method, String paramName) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (Annotation[] annotations : parameterAnnotations) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof CmdParam cmdParam) {
                    if (!paramName.equals(cmdParam.value())) {
                        continue;
                    }
                    return cmdParam.suggest();
                }
            }
        }
        return null;
    }

    /**
     * Gets the suggest method array by the suggest method name.
     * If the suggest method name ends with (), remove ().
     * Get the suggest method array from the current class first.
     * If the current class has the CmdSuggest annotation,
     * get the suggest method array from the CmdSuggest annotation.
     * <p>
     * 通过建议方法名称获取建议方法数组。
     * 如果建议方法名称以()结尾，则去掉()。
     * 优先从当前类中获取建议方法集合。
     * 如果当前类上有CmdSuggest注解，则从CmdSuggest注解中获取建议方法集合。
     *
     * @param suggestName The name of the suggest method. <br> 建议方法的名称
     * @return The suggest method array. <br> 建议方法数组
     */
    private Method[] getSuggestMethodByName(String suggestName) {
        if (suggestName.endsWith("()")) {
            suggestName = suggestName.substring(0, suggestName.length() - 2);
        }
        Method[] localSuggestMethod = getMethod(suggestName);
        if (localSuggestMethod != null && localSuggestMethod.length > 0) return localSuggestMethod;
        if (this.getClass().isAnnotationPresent(CmdSuggest.class)) {
            Class<?>[] value = this.getClass().getAnnotation(CmdSuggest.class).value();
            for (Class<?> clazz : value) {
                Method[] method = getMethod(clazz, suggestName);
                if (method != null) {
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * Gets the suggest method array by the suggest method name.
     * <p>
     * 通过建议方法名称获取建议方法数组。
     *
     * @param suggestName The name of the suggest method. <br> 建议方法的名称
     * @return The suggest method array. <br> 建议方法数组
     */
    @Nullable
    private Method[] getMethod(String suggestName) {
        return getMethod(this.getClass(), suggestName);
    }

    /**
     * Gets the suggest method array by the suggest method name from another class.
     * <p>
     * 从另一个类中通过方法名称获取方法数组。
     *
     * @param clazz       The class of the suggest method. <br> 建议方法的类
     * @param suggestName The name of the suggest method. <br> 建议方法的名称
     * @return The suggest method array. <br> 建议方法数组
     */
    @Nullable
    private Method[] getMethod(Class<?> clazz, String suggestName) {
        return ReflectUtil.getMethods(clazz, method -> method.getName().equals(suggestName));
    }

    /**
     * Invokes the suggest method. Inject parameters by parameter type.
     * <p>
     * 调用建议方法。按照参数类型注入参数。
     *
     * @param object  The object that the method is invoked from. <br> 方法所在的对象。
     * @param method  The method that is invoked. <br> 被调用的方法。
     * @param player  The player who will see the suggestions. <br> 看到补全的玩家
     * @param command The command that was typed in. <br> 需要补全的命令
     * @param strings The arguments of the command that was typed in. <br> 目前输入的命令参数
     * @return The return string list of suggestions of the method. <br> 方法的返回字符串列表。
     */
    private Collection<?> invokeSuggestMethod(Object object, Method method, Player player, Command command, String[] strings) {
        Parameter[] parameters = method.getParameters();
        Object[] params = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> type = parameter.getType();
            if (type.equals(Player.class)) {
                params[i] = player;
            } else if (type.equals(Command.class)) {
                params[i] = command;
            } else if (type.equals(String[].class)) {
                params[i] = strings;
            }
        }
        return ReflectUtil.invoke(object, method, params);
    }

    /**
     * Gets the methods that match the command.
     * Get the perfect match method first.
     * If there is no perfect match method, get the partial match method.
     * If there is no perfect match method and partial match method, return an empty list.
     * If there is a perfect match method, only return the perfect match method.
     * <p>
     * 获取匹配命令的方法。
     * 优先获取完全匹配的方法，如果没有完全匹配的方法，则获取部分匹配的方法。
     * 如果没有完全匹配的方法和部分匹配的方法，则返回空列表。
     * 如果有完全匹配的方法，则只返回完全匹配的方法。
     *
     * @param player  The player who will see the suggestions. <br> 看到补全的玩家
     * @param command The command that was typed in. <br> 需要补全的命令
     * @return The suggestions. <br> 补全的建议
     */
    private List<Method> getMethodsByArg(Player player, String command) {
        List<Method> perfectMatch = new ArrayList<>();
        List<Method> methods = new ArrayList<>();
        for (Map.Entry<String, Method> entry : mappings.entrySet()) {
            Method method = entry.getValue();
            String format = entry.getKey();
            if (format.startsWith(command.substring(0, command.lastIndexOf(" ")))) {
                if (checkPermission(player, method).isSuccess() && checkOp(player, method).isSuccess()) {
                    perfectMatch.add(method);
                }
            } else {
                String[] formatArgs = format.split(" ");
                String[] commandArgs = command.split(" ");
                boolean match = true;
                for (int i = 0; i < Math.min(commandArgs.length, formatArgs.length); i++) {
                    if (!matchesArgument(formatArgs[i], commandArgs[i])) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    if (checkPermission(player, method).isSuccess() && checkOp(player, method).isSuccess()) {
                        methods.add(method);
                    }
                }
            }
        }
        if (!perfectMatch.isEmpty()) {
            return perfectMatch;
        } else {
            return methods;
        }
    }

    /**
     * @return The help command. <br> 帮助命令。
     * @see #handleHelp(CommandSender)
     */
    protected String getHelpCommand() {
        return "help";
    }

    /**
     * Executes the command, returning its success.
     * <p>
     * 执行命令，返回是否成功。
     *
     * @param commandSender Source of the command <br> 命令的发送者
     * @param command       Command which was executed <br> 命令
     * @param s             Alias of the command which was used <br> 命令的别名
     * @param strings       Passed command arguments <br> 命令的参数
     * @return true if a valid command, otherwise false <br> 如果是有效的命令则返回true，否则返回false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 1 && getHelpCommand().equals(strings[0])) {
            handleHelp(commandSender);
            return true;
        }
        Method method = matchMethod(strings);
        if (method == null) {
            handleHelp(commandSender);
            return true;
        }
        // 检查参数长度
        CheckResponse checkResponse = checkParameters(strings, method, command);
        if (!checkResponse.isSuccess()) {
            handleParameterError(commandSender, checkResponse.getMessage());
            return true;
        }
        CheckResponse checkSenderResponse = checkSender(commandSender, method);
        if (!checkSenderResponse.isSuccess()) {
            handleSenderError(commandSender, checkSenderResponse.getMessage());
            return true;
        }
        CheckResponse checkPermissionResponse = checkPermission(commandSender, method);
        if (!checkPermissionResponse.isSuccess()) {
            handlePermissionError(commandSender, checkPermissionResponse.getMessage());
            return true;
        }
        CheckResponse checkOpResponse = checkOp(commandSender, method);
        if (!checkOpResponse.isSuccess()) {
            handleOpError(commandSender, checkOpResponse.getMessage());
            return true;
        }
        CheckResponse checkNotLockResponse = checkNotLock(commandSender, method);
        if (!checkNotLockResponse.isSuccess()) {
            handleNotLockError(commandSender, checkNotLockResponse.getMessage());
            return true;
        }
        CheckResponse checkNotInCDResponse = checkNotInCD(commandSender);
        if (!checkNotInCDResponse.isSuccess()) {
            handleNotInCDError(commandSender, checkNotInCDResponse.getMessage());
            return true;
        }
        Object[] params = buildParams(strings, method, commandSender);
        if (params == null) {
            return true;
        }
        BukkitRunnable bukkitRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                UsageLimit usageLimit = method.getAnnotation(UsageLimit.class);

                if (usageLimit != null) {
                    if (usageLimit.value().equals(UsageLimit.LimitType.ALL)) {
                        serverLock.put(((Player) commandSender).getUniqueId(), method);
                    } else if (usageLimit.value().equals(UsageLimit.LimitType.SENDER) && commandSender instanceof Player) {
                        senderLock.put(((Player) commandSender).getUniqueId(), method);
                    }
                }

                try {
                    setCoolDown(commandSender, method);
                    ReflectUtil.invoke(getInstance(), method, params);
                } finally {
                    if (usageLimit != null) {
                        if (usageLimit.value().equals(UsageLimit.LimitType.ALL)) {
                            serverLock.remove(((Player) commandSender).getUniqueId());
                        } else if (usageLimit.value().equals(UsageLimit.LimitType.SENDER) && commandSender instanceof Player) {
                            senderLock.remove(((Player) commandSender).getUniqueId());
                        }
                    }
                }
            }
        };
        if (method.isAnnotationPresent(RunAsync.class)) {
            bukkitRunnable.runTaskAsynchronously(plugin);
        } else {
            bukkitRunnable.runTask(plugin);
        }
        return true;
    }

    private CheckResponse checkParameters(String[] args, Method method, Command command) {
        // 从 mappings 中获取 method 对应的格式字符串
        String format = mappings.inverse().get(method);
        // 按空格分割格式字符串
        String[] formatArgs = format.split(" ");

        // 如果格式字符串中的参数数量与传入的参数数量不一致
        if (formatArgs.length != args.length) {
            int min = Math.min(formatArgs.length, args.length);

            // 如果传入的参数多于格式字符串中的参数
            if (formatArgs.length < args.length) {
                for (int i = 0; i < min; i++) {
                    // 检查最后一个格式参数是否是变长参数
                    if (formatArgs[formatArgs.length - 1].endsWith("...>")) {
                        return CheckResponse.SUCCESS;
                    }
                    // 如果当前参数不匹配
                    if (matchesArgument(formatArgs[i], args[i])) {
                        // 拼接传入的参数字符串
                        StringBuilder commandArgsStr = new StringBuilder(" ");
                        for (int j = 0; j < min; j++) {
                            commandArgsStr.append("§7").append(args[j]).append(" ");
                        }
                        // 告知错误位置
                        return new CheckResponse(
                                String.format(
                                        "§cArgument Error: §7/%s %s§c§n%s§r §c<--§o[Error Here]\b§eCorrect Usage: §7/%s %s",
                                        command.getName(),
                                        commandArgsStr,
                                        args[min],
                                        command.getName(),
                                        format
                                )
                        );
                    }
                }
            } else {
                // 如果传入的参数少于格式字符串中的参数
                // 拼接传入的参数字符串
                StringBuilder commandArgsStr = new StringBuilder(" ");
                for (int j = 0; j < min; j++) {
                    commandArgsStr.append("§7").append(args[j]).append(" ");
                }
                // 拼接缺少的参数字符串
                StringBuilder missingParameters = new StringBuilder();
                for (int j = min; j < formatArgs.length; j++) {
                    missingParameters.append("§c§n").append(formatArgs[j]).append(" ");
                }
                missingParameters = new StringBuilder(missingParameters.toString().trim());
                // 告知缺少参数的位置
                return new CheckResponse(
                        String.format(
                                "§cMissing Parameters: §7/%s %s§c§n%s§r §c<--§o[Missing Part]\b§eCorrect Usage: §7/%s %s",
                                command.getName(),
                                commandArgsStr,
                                missingParameters,
                                command.getName(),
                                format
                        )
                );
            }
        }
        return CheckResponse.SUCCESS;
    }

    /**
     * Requests a list of possible completions for a command argument.
     * <p>
     * 请求命令参数的可能补全列表。
     *
     * @param commandSender Source of the command.  For players tab-completing a
     *                      command inside of a command block, this will be the player, not
     *                      the command block.
     * @param command       Command which was executed
     * @param s             Alias of the command which was used
     * @param strings       The arguments passed to the command, including final
     *                      partial argument to be completed
     * @return A List of possible completions for the final argument, or null
     */
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Class<? extends AbstractCommendExecutor> clazz = this.getClass();
        if (!(commandSender instanceof Player)) {
            return null;
        }
        if (!clazz.isAnnotationPresent(CmdTarget.class)) {
            return null;
        }
        CmdTarget cmdTarget = clazz.getAnnotation(CmdTarget.class);
        if (cmdTarget.value().equals(CmdTarget.CmdTargetType.CONSOLE)) {
            return null;
        }
        return suggest((Player) commandSender, command, strings);
    }

    /**
     * Handles the error when there is a parameter issue.
     *
     * @param sender  The sender of the command.
     * @param message The error message to be sent.
     */
    public void handleParameterError(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.RED + message);
    }

    /**
     * Handles the error when the sender is invalid.
     *
     * @param sender  The sender of the command.
     * @param message The error message to be sent.
     */
    public void handleSenderError(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.RED + message);
    }

    /**
     * Handles the error when the sender lacks permission.
     *
     * @param sender  The sender of the command.
     * @param message The error message to be sent.
     */
    public void handlePermissionError(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.RED + message);
    }

    /**
     * Handles the error when the sender is not an operator (OP).
     *
     * @param sender  The sender of the command.
     * @param message The error message to be sent.
     */
    public void handleOpError(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.RED + message);
    }

    /**
     * Handles the error when the sender needs to wait for the previous command to finish.
     *
     * @param sender  The sender of the command.
     * @param message The error message to be sent.
     */
    public void handleNotLockError(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.RED + message);
    }

    /**
     * Handles the error when the sender needs to wait for the command cooldown.
     *
     * @param sender  The sender of the command.
     * @param message The error message to be sent.
     */
    public void handleNotInCDError(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.RED + message);
    }
}
