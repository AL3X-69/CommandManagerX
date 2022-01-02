package fr.alex6.discord.cmx.hook;

import fr.alex6.discord.cmx.CommandModule;

import java.lang.reflect.Method;

public class EventHandlerInfo {
    private final Class<?> event;
    private final CommandModule module;
    private final Method method;

    public EventHandlerInfo(Class<?> event, CommandModule module, Method method) {
        this.event = event;
        this.module = module;
        this.method = method;
    }

    public Class<?> getEvent() {
        return event;
    }

    public CommandModule getModule() {
        return module;
    }

    public Method getMethod() {
        return method;
    }
}
