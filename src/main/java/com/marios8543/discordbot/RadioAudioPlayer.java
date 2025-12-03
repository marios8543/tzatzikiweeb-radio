package com.marios8543.discordbot;

import com.marios8543.musicsource.Song;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class RadioAudioPlayer implements SongChangeListener {
    private static final String JF_TOKEN = System.getenv("JELLYFIN_TOKEN");
    private static final String BASE_URL = System.getenv("JELLYFIN_BASE_URL");
    public AudioPlayerManager manager = new DefaultAudioPlayerManager();
    public AudioPlayer audioPlayer = manager.createPlayer();

    @Override
    public void songChanged(Song newSong) {
        audioPlayer.stopTrack();
        String url = String.format("%s/Items/%s/Download?api_key=%s", BASE_URL, newSong.id, JF_TOKEN);
        manager.loadItem(url , new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                System.out.println("LOADED " + audioTrack);
                audioPlayer.startTrack(audioTrack, false);
            }
            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {}
            @Override
            public void noMatches() {
                System.out.println("NO MATCHES");
            }
            @Override
            public void loadFailed(FriendlyException e) { e.printStackTrace(); }
        });
    }
}
