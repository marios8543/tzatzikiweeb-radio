package com.marios8543;

import org.json.simple.JSONObject;

public class Song {
    final String path;
    public final String id;
    public final String title;
    public final String artist;
    public final String album;
    public final int length;
    public final String albumId;

    public Song(JSONObject obj){
        //System.out.println(obj.toJSONString());

        id = (String)obj.get("Id");
        path = sanitizePath(((String)obj.getOrDefault("Path", "")).replace("/media/Music", ""));
        title = (String)obj.get("Name");
        artist = (String)obj.getOrDefault("AlbumArtist", "Unknown Artist");
        album = (String)obj.getOrDefault("Album", "Unknown Album");
        length = (int) (((long)obj.getOrDefault("RunTimeTicks", 1))*(1./10000000.));
        albumId = (String) obj.getOrDefault("AlbumId", "");
    }

    private static String sanitizePath(String s) {
        //System.out.println(s);
        //return URLDecoder.decode(s, Charset.defaultCharset());
        return s;
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
