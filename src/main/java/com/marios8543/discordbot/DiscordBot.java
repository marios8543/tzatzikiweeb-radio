package com.marios8543.discordbot;

import com.marios8543.RadioApi;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class DiscordBot extends ListenerAdapter {
    private final JDA jda;
    private final RadioApi context;
    private final RadioAudioPlayer audioPlayer = new RadioAudioPlayer();

    public DiscordBot(String token, RadioApi ctx) {
        context = ctx;
        jda = JDABuilder.createDefault(token).build();
        jda.updateCommands().addCommands(
                Commands.slash("np", "Get current playing track"),
                Commands.slash("search", "Search song library")
                        .addOption(OptionType.STRING, "query", "Query", true),
                Commands.slash("connect", "Connect to voice channel")
                        .addOption(OptionType.CHANNEL, "channel", "Voice Channel", true),
                Commands.slash("upcoming", "Tracks coming up...")
        ).queue();
        AudioSourceManagers.registerRemoteSources(audioPlayer.manager);
        context.addSongChangeListener(audioPlayer);
        jda.addEventListener(new EventListener(context, jda, audioPlayer));
        context.addSongChangeListener(newSong -> {
            jda.getPresence().setActivity(Activity.listening(String.format("%s - %s", newSong.artist, newSong.title)));
        });
    }
}
