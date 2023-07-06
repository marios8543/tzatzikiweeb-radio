package com.marios8543.discordbot;

import com.marios8543.JellyfinClient;
import com.marios8543.RadioApi;
import com.marios8543.Song;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventListener extends ListenerAdapter {
    private static final String BASE_URL = System.getenv("JELLYFIN_BASE_URL");
    private final Map<String, Long> requestLimits = new HashMap<>();
    private final int requestLimit = Integer.parseInt(System.getenv("request_limit"));
    private final RadioApi context;
    private final JDA jda;
    private AudioPlayerDispatcher dispatcher;
    private List<String> channels = new ArrayList<>();

    public EventListener(RadioApi context, JDA jda, RadioAudioPlayer audioPlayer) {
        this.context = context;
        this.jda = jda;
        dispatcher = new AudioPlayerDispatcher(audioPlayer.audioPlayer);

        context.wsHandler.broadcast = (author, content) -> {
            for (String id : channels) {
                jda.getVoiceChannelById(id).sendMessage(author + ": " + content).queue();
            }
        };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "connect" -> {
                if (event.getGuild().getAudioManager().isConnected()) return;
                if (!event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) return;

                VoiceChannel channel = jda.getVoiceChannelById(event.getOption("channel", OptionMapping::getAsChannel).getId());
                event.getGuild().getAudioManager().setSendingHandler(dispatcher.getHandler());
                event.getGuild().getAudioManager().openAudioConnection(channel);
                channels.add(channel.getId());
                context.wsHandler.nowListening += channel.getMembers().size();
                channel.getMembers().forEach((member ->
                        context.wsHandler.broadcast("", member.getUser().getName() + " has joined the chat!")
                ));
                event.reply(":white_check_mark: Joined voice channel").setEphemeral(true).queue();
            }
            case "np" -> {
                Song nowPlaying = context.getNowPlaying();
                int durationSec = nowPlaying.length % 60;
                int durationMin = (nowPlaying.length / 60) % 60;
                int currentTime = context.getCurrentTime();
                int currentSec = currentTime % 60;
                int currentMin = (currentTime / 60) % 60;
                MessageEmbed embed = new EmbedBuilder().setTitle(nowPlaying.title)
                        .setAuthor(nowPlaying.artist)
                        .setDescription(nowPlaying.album)
                        .setFooter(String.format("%d:%d / %d:%d", currentMin, currentSec, durationMin, durationSec))
                        .setImage(String.format("%s/Items/%s/Images/Primary?maxHeight=300&quality=20", BASE_URL, nowPlaying.albumId)).build();
                event.replyEmbeds(embed).setEphemeral(true).queue();
            }
            case "search" -> {
                String query = event.getOption("query", OptionMapping::getAsString);
                try {
                    List<Song> results = JellyfinClient.search(query);
                    results = results.subList(0, Math.min(results.size(), 25));
                    StringSelectMenu.Builder menu = StringSelectMenu.create("search-results-" + event.getId());
                    for (Song song : results)
                        menu.addOption(String.format("%s - %s", song.artist, song.title), song.id);
                    event.reply("Request a song").addActionRow(menu.build()).queue();
                } catch (Exception e) {
                    event.reply("Something went wrong: " + e.getMessage()).setEphemeral(true).queue();
                }
            }
            case "upcoming" -> {
                EmbedBuilder embed = new EmbedBuilder().setTitle("Coming up next...");
                for (int i = 0; i < Math.min(context.getRequests().size(), 5); i++) {
                    Song s = context.getRequests().get(i);
                    embed.addField(s.title, s.artist + " - " + s.album, false);
                }
                for (int i = 0; i < Math.min(5 - Math.min(context.getRequests().size(), 5), context.getPlaylist().size()); i++) {
                    Song s = context.getPlaylist().get(i);
                    embed.addField(s.title, s.artist + " - " + s.album, false);
                }
                event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getComponentId().contains("search-results")) {
            String requestSongId = event.getValues().get(0);
            String requestUserId = event.getMember().getId();
            if (requestLimits.containsKey(requestUserId)) {
                long currentTime = Instant.now().getEpochSecond();
                if ((currentTime - requestLimits.get(requestUserId)) < requestLimit) {
                    event.reply("You can only make a song request every " + requestLimit + " seconds").setEphemeral(true).queue();
                    return;
                }
                else requestLimits.remove(requestUserId);
            }
            try {
                Song song = JellyfinClient.getSongById(requestSongId);
                context.addRequest(song);
                requestLimits.put(requestUserId, Instant.now().getEpochSecond());
                event.reply(String.format("You requested %s - %s. Position: %d", song.artist, song.title, context.getRequests().size())).setEphemeral(true).queue();
            } catch (Exception e) {
                event.reply("Something went wrong!").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        Member member = event.getEntity();
        if (member.getUser().equals(event.getJDA().getSelfUser())) return;
        AudioChannel oldChannel = event.getOldValue();
        AudioChannel newChannel = event.getNewValue();
        if ((oldChannel == null && newChannel != null) || (oldChannel != null && newChannel != null && !oldChannel.equals(newChannel))) {
            if (channels.contains(newChannel.getId())) {
                context.wsHandler.nowListening++;
                context.wsHandler.broadcast("", member.getUser().getName() + " has joined the chat!");
            }
        } else if (oldChannel != null && newChannel == null) {
            if (channels.contains(oldChannel.getId())) {
                context.wsHandler.nowListening--;
                context.wsHandler.broadcast("", member.getUser().getName() + " has left the chat!");
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!channels.contains(event.getChannel().getId())) return;
        if (event.getAuthor().equals(event.getJDA().getSelfUser())) return;
        context.wsHandler.broadcast(event.getAuthor().getName(), event.getMessage().getContentDisplay());
    }
}
