package fr.alex6.discord.cmx.examples;

import fr.alex6.discord.cmx.CommandManager;
import fr.alex6.discord.cmx.impl.TestModule;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;

public class Main {
    public static void main(String[] args) throws LoginException {
        CommandManager manager = new CommandManager("!");
        manager.registerModule(new TestModule());
        JDABuilder.createLight(System.getProperty("bot.token")).addEventListeners(manager).build();
    }
}
