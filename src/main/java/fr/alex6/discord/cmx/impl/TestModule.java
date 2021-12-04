package fr.alex6.discord.cmx.impl;

import fr.alex6.discord.cmx.Command;
import fr.alex6.discord.cmx.CommandModule;
import net.dv8tion.jda.api.entities.Message;

import java.util.Arrays;

public class TestModule extends CommandModule {
    @Command(value = "test", aliases = "t")
    public void test(Message message, String arg, String[] args) {
        message.reply("Ok ! You sent: "+arg+" | "+ Arrays.toString(args)).queue();
    }
}
