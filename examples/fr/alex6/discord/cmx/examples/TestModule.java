package fr.alex6.discord.cmx.examples;

import fr.alex6.discord.cmx.Command;
import fr.alex6.discord.cmx.CommandModule;
import fr.alex6.discord.cmx.hook.Event;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;

public class TestModule extends CommandModule {
    @Command(value = "test", aliases = "t")
    public void test(Message message, String arg, String[] args) {
        message.reply("Ok ! You sent: "+arg+" | "+ Arrays.toString(args)).queue();
    }

    @Event
    public void onMessage(MessageReceivedEvent event) {
        System.out.println("I received the following message: "+event.getMessage().getContentRaw());
    }
}
