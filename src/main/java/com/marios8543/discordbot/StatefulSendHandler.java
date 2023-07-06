package com.marios8543.discordbot;

import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.ByteBuffer;

public class StatefulSendHandler implements AudioSendHandler {
    int bufferIndex;
    AudioPlayerDispatcher dispatcher;
    public StatefulSendHandler(int idx, AudioPlayerDispatcher dispatcher) {
        bufferIndex = idx;
        this.dispatcher = dispatcher;
    }
    @Override
    public boolean canProvide() {
        return dispatcher.frameList.size() > bufferIndex;
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        ByteBuffer buffer = ByteBuffer.wrap(dispatcher.frameList.get(bufferIndex).getData());
        bufferIndex++;
        return buffer;
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}
