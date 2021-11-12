package fr.alex6.discord.cmx.impl;

import fr.alex6.discord.cmx.Command;
import fr.alex6.discord.cmx.CommandModule;
import net.dv8tion.jda.api.entities.Message;

public class TestModule extends CommandModule {
    @Command("test")
    public void test(Message message) {
        message.reply("Ok !").queue();
    }
}
