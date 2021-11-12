package fr.alex6.discord.cmx;

import java.lang.reflect.Method;

/**
 *
 */
public class CommandInfo {
    private final Command command;
    private final Method method;
    private final CommandModule module;

    protected CommandInfo(Command command, Method method, CommandModule module) {
        this.command = command;
        this.method = method;
        this.module = module;
    }

    public Command getCommand() {
        return command;
    }

    public Method getMethod() {
        return method;
    }

    public CommandModule getModule() {
        return module;
    }
}
