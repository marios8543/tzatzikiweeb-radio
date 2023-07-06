package com.marios8543.discordbot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

import java.util.ArrayList;
import java.util.List;

public class AudioPlayerDispatcher {
    List<AudioFrame> frameList = new ArrayList<>();
    List<StatefulSendHandler> sendHandlers = new ArrayList<>();
    int currentFrameIndex = 0;
    public AudioPlayerDispatcher(AudioPlayer player) {
        player.addListener(new AudioEventAdapter() {
            @Override
            public void onTrackStart(AudioPlayer player, AudioTrack track) {
                super.onTrackStart(player, track);
                frameList.clear();
                currentFrameIndex = 0;
                sendHandlers.forEach(handler -> handler.bufferIndex = 0);
            }
        });
        new Thread(() -> {
            while (true) {
                AudioFrame frame = player.provide();
                if (frame != null) {
                    frameList.add(frame);
                    currentFrameIndex++;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public StatefulSendHandler getHandler() {
        StatefulSendHandler handler = new StatefulSendHandler(currentFrameIndex, this);
        sendHandlers.add(handler);
        return handler;
    }
}
