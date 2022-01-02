package fr.alex6.discord.cmx;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.alex6.discord.cmx.hook.Event;
import fr.alex6.discord.cmx.hook.EventHandlerInfo;
import fr.alex6.discord.cmx.jackson.ColorJsonDeserializer;
import fr.alex6.discord.cmx.jackson.ColorJsonSerializer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.utils.ClassWalker;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;

/**
 * CommandManager class, used to map the commands, use <code>registerModule()</code> to register a new Module
 *
 * @author Alex6
 */
public class CommandManager extends ListenerAdapter {
    private final List<CommandModule> modules = new ArrayList<>();
    private final Map<String, CommandInfo> commands = new ConcurrentHashMap<>();
    private final Map<Class<?>, Set<EventHandlerInfo>> eventHandlers = new ConcurrentHashMap<>();
    private final String prefix;
    private final ScheduledExecutorService scheduledExecutorService;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    /**
     * Create a new instance of CommandManager with the selected prefix
     * @param prefix The prefix used by the bot
     */
    public CommandManager(@NotNull String prefix) {
        Objects.requireNonNull(prefix);
        if (prefix.equals("")) throw new NullPointerException("Prefix should not be empty");
        this.prefix = prefix;
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        registerCustomJacksonModules(objectMapper);
        this.objectMapper = objectMapper;
        this.cacheManager = new CacheManager(this.objectMapper);
    }

    private BiConsumer<MessageReceivedEvent, Permission[]> permissionsErrorHandler = (messageReceivedEvent, permissions) -> messageReceivedEvent.getMessage().reply(":x: Missing permissions: "+ Arrays.toString(permissions)).queue();
    private BiConsumer<MessageReceivedEvent, Throwable> runtimeErrorHandler = (messageReceivedEvent, throwable) -> messageReceivedEvent.getMessage().reply(":x: An error occurred: "+throwable.getMessage()).queue();

    private void registerCustomJacksonModules(ObjectMapper objectMapper) {
        SimpleModule awtModule = new SimpleModule("AWT Module");
        awtModule.addSerializer(Color.class, new ColorJsonSerializer());
        awtModule.addDeserializer(Color.class, new ColorJsonDeserializer());

        objectMapper.registerModules(awtModule);
    }

    /**
     * Register a new command module to the manager, all method annotated with the <code>@{@link Command}</code> annotation will be mapped
     *
     * @param module A class extending from the <code>{@link CommandModule}</code> class with the commands to be registered
     * @throws IllegalArgumentException when a specified command/alias is already mapped with the same name
     */
    public void registerModule(@NotNull CommandModule module) {
        Objects.requireNonNull(module);
        mapCommands(module);
        mapEvents(module);
        modules.add(module);
        module.onRegister();
        module.registerTasks(scheduledExecutorService);
    }

    public void registerModules(@NotNull CommandModule... modules) {
        for (CommandModule module : modules) {
            registerModule(module);
        }
    }

    /**
     * Return an immutable list of all modules
     *
     * @return an immutable list of registered {@link CommandModule}
     */
    public List<CommandModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    /**
     * Get a command by her name/alias
     *
     * @param command The name or alias of the command
     * @return The <code>{@link CommandInfo}</code> associated with the mapped command or null if the command isn't mapped
     */
    public CommandInfo getCommand(String command) {
        return commands.get(command);
    }

    /**
     * Set the function executed when a user use a command but has insufficient permissions
     *
     * @param permissionsErrorHandler A <code>{@link BiConsumer}</code>
     */
    public void setPermissionsErrorHandler(@NotNull BiConsumer<MessageReceivedEvent, Permission[]> permissionsErrorHandler) {
        Objects.requireNonNull(permissionsErrorHandler);
        this.permissionsErrorHandler = permissionsErrorHandler;
    }

    /**
     * Set the function executed when a user use a command but the command method throw a <code>{@link RuntimeException}</code>
     *
     * @param runtimeErrorHandler A <code>{@link BiConsumer}</code>
     */
    public void setRuntimeErrorHandler(@NotNull BiConsumer<MessageReceivedEvent, Throwable> runtimeErrorHandler) {
        this.runtimeErrorHandler = runtimeErrorHandler;
    }

    private void mapCommands(@NotNull CommandModule module) {
        for (Method method : module.getClass().getMethods()) {
            if (method.isAnnotationPresent(Command.class)) {
                Command command = method.getAnnotation(Command.class);
                if (commands.containsKey(command.value())) throw new IllegalArgumentException("Duplicated command: Command "+command.value()+" is already mapped");
                commands.put(command.value(), new CommandInfo(command, method, module));
                for (String alias : command.aliases()) {
                    if (commands.containsKey(alias)) throw new IllegalArgumentException("Duplicated command: Alias "+command.value()+" is already mapped");
                    commands.put(alias, new CommandInfo(command, method, module));
                }
            }
        }
    }

    private void mapEvents(@NotNull CommandModule module) {
        for (Method method : module.getClass().getMethods()) {
            if (method.isAnnotationPresent(Event.class)) {
                if (method.getParameterCount() == 0) throw new IllegalArgumentException("The event annotation must be followed by a method with a parameter extending GenericEvent");
                Class<?> eventClass = method.getParameters()[0].getType();
                if (!eventHandlers.containsKey(eventClass)) eventHandlers.put(eventClass, ConcurrentHashMap.newKeySet());
                eventHandlers.get(eventClass).add(new EventHandlerInfo(eventClass, module, method));
            }
        }
    }

    public void addJacksonModule(Module module) {
        objectMapper.registerModule(module);
    }

    public void addJacksonModules(Module... modules) {
        objectMapper.registerModules(modules);
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().equals(event.getJDA().getSelfUser())) return;
        if (event.getMessage().getContentRaw().startsWith(prefix)) {
            String command = event.getMessage().getContentRaw().substring(prefix.length()).split(" ")[0];
            String[] args = event.getMessage().getContentRaw().substring(prefix.length()).split(" ", 2).length == 1 ? new String[0] : event.getMessage().getContentRaw().substring(prefix.length()).split(" ", 2)[1].split(" ");
            if (commands.containsKey(command)) {
                CommandInfo commandInfo = commands.get(command);
                if ((event.getAuthor().isBot() && !commandInfo.getCommand().replyToBot()) || event.isWebhookMessage()) return;
                if (event.getChannel() instanceof PrivateChannel && commandInfo.getCommand().executionRule() == ExecutionRule.TEXT_CHANNEL) return;
                if (event.getChannel() instanceof TextChannel && commandInfo.getCommand().executionRule() == ExecutionRule.PRIVATE) return;
                if (event.getChannel() instanceof TextChannel) {
                    assert event.getMember() != null;
                    if (!event.getMember().getPermissions().containsAll(Arrays.asList(commandInfo.getCommand().permissions()))) {
                        permissionsErrorHandler.accept(event, (Permission[]) Arrays.stream(commandInfo.getCommand().permissions()).filter(event.getMember().getPermissions()::contains).toArray());
                        return;
                    }
                }

                // All verified, execution
                Method method = commandInfo.getMethod();
                Parameter[] parameters = method.getParameters();
                Object[] values = new Object[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i].getType() == String[].class) values[i] = args;
                    else if (parameters[i].getType() == String.class) values[i] = String.join(" ", args);
                    else if (parameters[i].getType() == User.class) values[i] = event.getAuthor();
                    else if (parameters[i].getType() == Member.class) values[i] = event.getMember();
                    else if (parameters[i].getType() == MessageReceivedEvent.class) values[i] = event;
                    else if (parameters[i].getType() == MessageChannel.class) values[i] = event.getChannel();
                    else if (parameters[i].getType() == TextChannel.class) values[i] = event.getChannel() instanceof TextChannel ? (TextChannel) event.getChannel() : null;
                    else if (parameters[i].getType() == PrivateChannel.class) values[i] = event.getChannel() instanceof PrivateChannel ? (PrivateChannel) event.getChannel() : event.getAuthor().openPrivateChannel().complete();
                    else if (parameters[i].getType() == Guild.class) values[i] = event.getChannel() instanceof TextChannel ? event.getGuild() : null;
                    else if (parameters[i].getType() == Message.class) values[i] = event.getMessage();
                    else if (parameters[i].getType() == JDA.class) values[i] = event.getJDA();
                    else if (parameters[i].getType() == ShardManager.class) values[i] = event.getJDA().getShardManager();
                    else if (parameters[i].getType() == CacheManager.class) values[i] = cacheManager;
                    else if (parameters[i].getType() == ObjectMapper.class) values[i] = objectMapper;
                    else if (parameters[i].getType() == Boolean.class) {
                        if (parameters[i].getName().equals("private")) values[i] = event.getTextChannel() instanceof PrivateChannel;
                        else if (parameters[i].getName().equals("bot")) values[i] = event.getAuthor().isBot();
                        else values[i] = null;
                    }
                    else values[i] = null;
                }
                try {
                    method.invoke(commandInfo.getModule(), values);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.getTargetException().printStackTrace();
                    runtimeErrorHandler.accept(event, e.getTargetException());
                }
            }
        }
    }

    @Override
    public void onGenericEvent(@NotNull GenericEvent event) {
        for (Class<?> eventClass : ClassWalker.range(event.getClass(), GenericEvent.class)) {
            if (!eventHandlers.containsKey(eventClass)) continue;
            for (EventHandlerInfo eventHandler : eventHandlers.get(eventClass)) {
                try {
                    eventHandler.getMethod().invoke(eventHandler.getModule(), event);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.getTargetException().printStackTrace();
                }
            }
        }


    }
}
