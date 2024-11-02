# ⌨️ UltiTools-Command 🛠️

[![Maven Central](https://img.shields.io/maven-central/v/com.ultikits.lib/UltiKits-Command?label=Maven%20Central)](https://search.maven.org/artifact/com.ultikits.lib/UltiKits-Command)
[![GitHub](https://img.shields.io/github/license/UltiKits/UltiKits-Command)](https://github.com/UltiKits/UltiKits-Command?tab=MIT-1-ov-file#readme)
[![GitHub](https://img.shields.io/github/issues/UltiKits/UltiKits-Command)](https://github.com/UltiKits/UltiKits-Command/issues)
[![CodeFactor](https://www.codefactor.io/repository/github/ultikits/ultikits-command/badge)](https://www.codefactor.io/repository/github/ultikits/ultikits-command)
[![GitHub](https://img.shields.io/github/forks/UltiKits/UltiKits-Command)](https://github.com/UltiKits/UltiKits-Command/network/members)
[![GitHub](https://img.shields.io/github/stars/UltiKits/UltiKits-Command)](https://github.com/UltiKits/UltiKits-Command/stargazers)

In traditional Bukkit plugin development, we usually use the `CommandExecutor` interface of Bukkit to handle commands.

However, in some cases, we need to determine whether the sender of the command is a player, whether it has certain
permissions, and determine the parameters, etc.

If a plugin has multiple commands, then these judgment logic will be repeated in the processing method of each command,
such code is very redundant.

In addition, we may also need to handle command errors, output help information, etc.

UltiTools-Command offers a more concise way to handle commands by encapsulating the native `CommandExecutor` interface.

# Quick start

## Installation

### Maven

```xml
<dependency>
    <groupId>com.ultikits.lib</groupId>
    <artifactId>UltiKits-Command</artifactId>
    <version>1.0.2</version>
</dependency>
```

### Gradle

```groovy
dependencies {
    implementation 'com.ultikits.lib:UltiKits-Command:1.0.2'
}
```

## Usage

Assuming that your plugin has a function to set the teleport point, you want the player to enter a command with the
teleport point name, to set up a teleport point.

Then this command should look like this: `/point add [name]`

If you use the traditional method, you need to judge the legality of the parameter input, the sender and permissions,
etc. If there are other functions, you also need to write a lot of `switch ... case` and `if ... else` statements, crazy
nesting.

However, with UltiTools-Command, you only need to write the main logic, and the rest will be handled automatically.

First, you need to create an executor class that inherits `AbstractCommandExecutor`.

```java
import com.ultikits.lib.command.AbstractCommendExecutor;
import com.ultikits.lib.annotations.command.CmdExecutor;
import com.ultikits.lib.annotations.command.CmdTarget;
import org.bukkit.command.CommandSender;

// Command limits executor
@CmdTarget(CmdTarget.CmdTargetType.PLAYER)
@CmdExecutor(
        // Command permission (optional)
        permission = "ultikits.example.all",
        // Command description (optional)
        description = "Test Command",
        // Command alias
        alias = {"point"},
        // Whether to register manually (optional)
        manualRegister = false,
        // Whether to require OP permission (optional)
        requireOp = false
)
public class PointCommand extends AbstractCommendExecutor {

    @Override
    protected void handleHelp(CommandSender sender) {
        // Send help message to command sender
    }
}
```

Then create a method named `addPoint` and add the parameters you want:

```java
public void addPoint(@CmdSender Player player, String name) {
    // Your code
}
```

Yes, each of your functions uses a separate function without extra judgment.

Then, you need to add the `@CmdMapping` annotation to match your method according to the input
command:

```java

@CmdMapping(format = "add <name>")
public void addPoint(@CmdSender Player player, String name) {
    // Your code
}
```

Finally, use `@CmdParam` to bind command parameters:

```java

@CmdMapping(format = "add <name>")
public void addPoint(@CmdSender Player player, @CmdParam("name") String name) {
    // Your code
}
``` 

Full code:
    
```java
import com.ultikits.lib.command.AbstractCommendExecutor;
import com.ultikits.lib.annotations.command.CmdExecutor;
import com.ultikits.lib.annotations.command.CmdTarget;
import org.bukkit.command.CommandSender;

// Command limits executor
@CmdTarget(CmdTarget.CmdTargetType.PLAYER)
@CmdExecutor(
        // Command permission (optional)
        permission = "ultikits.example.all",
        // Command description (optional)
        description = "Test Command",
        // Command alias
        alias = {"point"},
        // Whether to register manually (optional)
        manualRegister = false,
        // Whether to require OP permission (optional)
        requireOp = false
)
public class PointCommand extends AbstractCommendExecutor {

    @CmdMapping(format = "add <name>")
    public void addPoint(@CmdSender Player player, @CmdParam("name") String name) {
        // Your code
    }

    @Override
    protected void handleHelp(CommandSender sender) {
        // Send help message to command sender
    }
}
```

Now, you can register the command executor in your main class by using ```CommandManager``` to complete all the work.

```java
import com.ultikits.lib.command.CommandManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginMain extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register command executor
        CommandManager.registerCommand(new PointCommand());
    }
}
```

Yeah! You can now use the `/point add [name]` command in the game.

### Tab completion

Need Tab suggestion for each command parameter, but don't want to write a lot of code?

It is a disaster to generate a completion list by judging the length of each command and the previous parameters.

Now you only need to write a method for each parameter to return a completion list! This method can be reused, and all
the complicated parameter quantity judgments are left to UltiTools to complete.

What you need to do is just add the `suggest` attribute in the `@CmdParam` annotation and specify a method name.

```java

@CmdMapping(format = "add <name>")
public void addPoint(@CmdSender Player player, @CmdParam(value = "name", suggest = "listName") String name) {
    // Your code
}

public List<String> listName(Player player, Command command, String[] args) {
    // Your code
}
```

UltiTools will first search for matching method names in the current class and try to call this method.

Your method can contain up to three parameters, corresponding to the types `Player`, `Command` and `String[]`. You can
choose any amount or order of parameters, but the type can only be these three types, one parameter for each type.

`Player` represents the player who sent the command, `Command` represents the current command, and `String[]` represents
the current parameters of the current command.

Your method needs to return a value of type `List<String>`, and UltiTools will return this value as a completion list to
the player.

If you just want to return a simple prompt string, then you only need to write the string you want in the `suggest`
field. The string here also supports internationalization.

```java

@CmdMapping(format = "add <name>")
public void addPoint(@CmdSender Player player,
                     @CmdParam(value = "name", suggest = "[name]") String name) {
    // Your code
}

```

If you are not satisfied with the completion list generated by UltiTools, you can override the `suggest` method to
generate the completion list yourself.

```java

@Override
protected List<String> suggest(Player player, Command command, String[] strings) {
    // Your code
}
```

#### @CmdSuggest

If you want a completion method to be shared with other command classes, you can create a class and write methods which
you want to reuse in other class.

Add the `@CmdSuggest` annotation to the class which need to use suggestion method, and specify the suggestion class.

```java

@CmdSuggest({PointSuggest.class})
public class PointCommand extends AbstractCommandExecutor {

    @CmdMapping(format = "add <name>")
    public void addPoint(@CmdSender Player player, @CmdParam(value = "name", suggest = "listName") String name) {
        // Your code
    }
}
```

```java
public class PointSuggest {
    public List<String> listName(Player player, Command command, String[] args) {
        // Your code
    }
}
```

### Parameters

#### Command without Parameters

If a command does not require any parameters, simply leave the `format` value empty.

```java
@CmdMapping(format="")
```

This type of command can have at most one occurrence.

#### Variable Parameters

For the last parameter in a method, you can use an array type by adding `...` to the last parameter in the `format`. Here's an example:

```java
@CmdMapping(format = "add <name...>")
public void addPoint(@CmdSender Player player, @CmdParam(value = "name...") String[] name) {
    // Your code
}
```

In this example, when a player enters `/somecmd add aa bb cc`, the `name` will be `['aa', 'bb', 'cc']`.

#### Type Parsing

Before passing parameters to a method, UltiTools converts the command's variable parameters based on the types required by the method.

All parsers are stored in a map called `parsers`, and you can use `getParser()` to access it.

For some types, `AbstractCommandExecutor` provides default parsers (including base types and arrays):

- String (Java built-in)
- Float (Java built-in)
- Double (Java built-in)
- Integer (Java built-in)
- Short (Java built-in)
- Byte (Java built-in)
- Long (Java built-in)
- OfflinePlayer (Bukkit API)
- Player (Bukkit API)
- Material (Bukkit API)
- UUID (Java built-in)
- Boolean (Java built-in)

If you want to use a custom parser, you need to create a method that can be used with the `Function` interface.

Supported parser types are `<String, ?>`, meaning the method has exactly one parameter of type `String` and returns a value of any type.

```java
public static SomeType toSomeType(String s) {
  //do something...
  return result;
}
```

Then, add the converter in the constructor:

```java
public SomeCommand() {
  super();
  getParsers().put(Arrays.asList(SomeType.class, SomeType[].class), SomeType::toSomeType);
}
```

Make sure to add the array type as well; otherwise, variable parameters won't be parsed.

### Permission

#### Method permission

If you need to specify permissions for a method, you need to add the `permission` attribute in the `@CmdMapping`
annotation.

```java
@CmdMapping(..., permission = "point.set.add")
```

The permissions specified in `@CmdExecutor` will override any permission set in `@CmdMapping`.

#### OP Required

If you want all methods to be executed by OP only, you need to set the `requireOp` attribute in `@CmdExecutor` to `true`

```java
@CmdExecutor(..., requireOp = true)
```

If you want a method to be executed by OP only, you need to set the `requireOp` attribute in `@CmdMapping` to `true`

```java
@CmdMapping(..., requireOp = true)
```

### Sender Limitation

If you want to specify the sender for all methods, you need to add the `@CmdTarget` annotation in front of your
class.

If you want to specify the sender for a method, just add it in front of the method.

```java
@CmdTarget(CmdTarget.CmdTargetType.BOTH)
```

If the sender is specified in both the class and the method, both must be met.

### Asynchronous Execution

If a command needs to execute a task that takes a long time, you need to add `@RunAsync` in front of the corresponding

```java

@CmdMapping(format = "list")
@RunAsync
public void listPoint(@CmdSender Player player) {
    //do query
}
```

This will create a new asynchronous thread to execute the method, avoiding blocking in the Bukkit main thread.

Since the Bukkit API does not allow asynchronous calls, if you need to call the Bukkit API, you need to create a
synchronous task:

```java

@CmdMapping(format = "list")
@RunAsync
public void listPoint(@CmdSender Player player) {
    //do query
    new BukkitRunnable() {
        @Override
        public void run() {
            //call bukkit api
        }
    }.runTask(PluginMain.getInstance());
}
```

### Command cooldown

If you don't want a command to be executed in large quantities and consume server resources, then you can add
`@CmdCD` in front of the corresponding method:

```java
@CmdCD(60)
```

Parameter type is integer, in second.

If the command is executed before the cooldown ends, the message `Frequent operations, please try again later` will be
sent.

This restriction only takes effect on **players**.

### Execution lock

If you want a command to be executed only one by one, you can add `@UsageLimit` in front of the corresponding method:

```java
@UsageLimit(ContainConsole = false, value = LimitType.SENDER)
```

`ContainConsole` is whether the restriction is applied to the console, and `value` is the restriction type.

Available types are:

- `LimitType.SENDER` limits that each sender can only have one command of this type executed at a time
- `LimitType.ALL` limits that only one command of this type can be executed in the whole server
- `LimitType.NONE` no limit

Under the `LimitType.SENDER` strategy, the player will receive a prompt: `Please wait for last Command Processing!`

Under the `LimitType.ALL` strategy, the player will receive a
prompt: `Please wait for last Command Processing which sent by other players!`

## Vanilla Bukkit Command Executor Wrapper

### Player Command

If you want a command to be executed only in the game (executed by the player), you can inherit the
`AbstractPlayerCommandExecutor` class and override the `onPlayerCommand` method.

```java
public class SomeCommands extends AbstractPlayerCommandExecutor {
    @Override
    protected boolean onPlayerCommand(Command command, String[] strings, Player player) {
        // your code
        return true;
    }
}
```

Except for the `Player` type parameter, this method is the same as the `CommandExecutor#onCommand` method.

If you try to execute this command in the console, you will receive an error
message: `This command can only be performed in GAME!`

If you want this command to use Tab completion, please see the next section.

### Command Completion

From Minecraft 1.13, the Bukkit API provides a new `TabCompleter` interface for command completion.

UltiTools has encapsulated this interface to provide a more concise way of command completion.

You need to inherit the `AbstractTabExecutor` class and override the `onTabComplete` method.

```java

@Override
protected List<String> onPlayerTabComplete(Command command, String[] strings, Player player) {
    // your code
    return null;
}
```

Except for the `Player` type parameter, this method is the same as the `TabCompleter#onTabComplete` method.

The rest of the usage is the same as the `AbstractPlayerCommandExecutor` class.

### Console Command

If you want a command to be executed only in the console, you can inherit the `AbstractConsoleCommandExecutor` class and
override the `onConsoleCommand` method.

```java
public class SomeCommands extends AbstractConsoleCommandExecutor {
    @Override
    protected boolean onConsoleCommand(CommandSender commandSender, Command command, String[] strings) {
        // your code
        return true;
    }
}
```

This method is the same as the `CommandExecutor#onCommand` method.

If you try to execute this command in the game, you will receive an error
message: `This command can only be performed in CONSOLE!`

### Help Message

All three classes above provide a `sendHelpMessage` method for sending help messages to players or consoles.

```java
sendHelpMessage(CommandSender sender) {
    // send help message
}
```

When sending the `/somecommand help` command, this method will be called.

### Error Message

You may find that the `onCommand` method of the three classes above returns a `boolean` type value.

It is the same as the native `CommandExecutor` interface, this value is used to indicate whether the command was
executed successfully.

When the command execution returns `false`, the command sender will be automatically prompted with an error message.
