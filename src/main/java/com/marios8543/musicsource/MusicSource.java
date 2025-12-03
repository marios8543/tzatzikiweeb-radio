package com.marios8543.musicsource;

public interface MusicSource {
    ChunkedLibrary search(String query, String sort, int offset) throws MusicSourceException;
    ChunkedLibrary search(String query) throws MusicSourceException;
    Song getSongById(String id) throws MusicSourceException;
    Song getRandomSong() throws MusicSourceException;
    ChunkedLibrary getLibrary(int offset, String sort) throws MusicSourceException;
}
