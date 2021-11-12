package fr.alex6.discord.cmx;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledExecutorService;

public abstract class CommandModule {
    public void onRegister() {}
    public void registerTasks(@NotNull ScheduledExecutorService scheduledExecutorService) {}
}
