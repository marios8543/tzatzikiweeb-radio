package com.marios8543.musicsource;

public class ChunkedLibrary {
    public Song[] items;
    public long totalCount;

    public ChunkedLibrary(Song[] s, int t) {
        items = s;
        totalCount = t;
    }
}
