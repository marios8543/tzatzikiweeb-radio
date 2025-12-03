package com.marios8543.musicsource;

import org.json.simple.JSONObject;

public class Song {
    final String path;
    public final String id;
    public final String title;
    public final String artist;
    public final String album;
    public final int length;
    public final String albumId;

    public Song(String path, String id, String title, String artist, String album, int length, String albumId) {
        this.path = path;
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.length = length;
        this.albumId = albumId;
    }

    public Song(JSONObject obj){
        id = (String)obj.get("id");
        path = ((String)obj.getOrDefault("path", "")).replace("/music", "");
        title = (String)obj.get("title");
        artist = (String)obj.getOrDefault("artist", "Unknown Artist");
        album = (String)obj.getOrDefault("album", "Unknown Album");
        length = Math.toIntExact((long)obj.getOrDefault("duration", 1));
        albumId = (String) obj.getOrDefault("albumId", "");
    }

    /** @noinspection unchecked, unchecked */
    public JSONObject toJSON(){
        JSONObject ret = new JSONObject();
        ret.put("path",path);
        ret.put("title",title);
        ret.put("artist",artist);
        ret.put("album",album);
        ret.put("length",length);
        ret.put("id",id);
        ret.put("albumId", albumId);
        return ret;
    }

    public String toString(){
        return String.format("%s -%s (%s)",artist,title,album);
    }
}
