package com.company;


import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class JellyfinClient {
    private static final String BASE_URL = System.getenv("JELLYFIN_BASE_URL");
    private static final String TOKEN = System.getenv("JELLYFIN_TOKEN");
    private static final String USER_ID = System.getenv("JELLYFIN_USER_ID");
    private static final String MUSIC_LIBRARY_ID = System.getenv("JELLYFIN_MUSIC_LIBRARY_ID");
    private static final String RADIO_PLAYLIST_ID = System.getenv("JELLYFIN_RADIO_PLAYLIST_ID");

    private static final String HEADER = String.format("MediaBrowser Client=\"Jellyfin CLI\", Device=\"Jellyfin-CLI\", DeviceId=\"None\", Version=\"10.4.3\", Token=\"%s\"", TOKEN);
    private static final HttpClient client = HttpClientBuilder.create().
            setDefaultHeaders(Arrays.stream(new Header[] {new BasicHeader("x-emby-authorization", HEADER)}).toList()).build();

    public static class JellyfinHttpError extends Exception {
        public JellyfinHttpError(String message) {
            super(message);
        }
    }

    public static class ChunkedLibrary {
        public Song[] items;
        public long totalCount;

        public ChunkedLibrary(Song[] s, int t) {
            items = s;
            totalCount = t;
        }
    }

    private static final ChunkedLibrary tmpChunkedLibrary = new ChunkedLibrary(null, 0);

    private static JSONArray jellyfinBaseRequest(String reqType, String reqTypeValue, URI uri) throws JellyfinHttpError, IOException, ParseException {
        String url = String.format("%s/%s/%s/Items%s", BASE_URL, reqType, reqTypeValue, uri.toString());
        HttpGet get = new HttpGet(url);
        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() == 200) {
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
            if (jsonObject.containsKey("TotalRecordCount")) tmpChunkedLibrary.totalCount = (Long) jsonObject.get("TotalRecordCount");
            return (JSONArray) jsonObject.get("Items");
        }
        throw new JellyfinHttpError(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
    }

    private static JSONArray jellyfinItemsRequest(URI uri) throws JellyfinHttpError, IOException, ParseException {
        return jellyfinBaseRequest("Users", USER_ID, uri);
    }

    private static JSONArray jellyfinPlaylistRequest(URI uri) throws JellyfinHttpError, IOException, ParseException {
        return jellyfinBaseRequest("Playlists", RADIO_PLAYLIST_ID, uri);
    }

    private static Song[] jsonArrayToSongArray(JSONArray array) {
        Song[] songs = new Song[array.size()];
        for (int i = 0; i < array.size(); i++) {
            songs[i] = new Song((JSONObject) array.get(i));
        }
        return songs;
    }

    public static ChunkedLibrary getLibrary(int offset, String sort) throws JellyfinHttpError, IOException, ParseException, URISyntaxException {
        URI uri = new URIBuilder().
                addParameter("UserId", USER_ID).
                addParameter("SortBy", sort).
                addParameter("Fields", "AudioInfo,BasicSyncInfo,Path,RuntimeTicks").
                addParameter("Limit", "30").
                addParameter("StartIndex", String.format("%d", offset * 30)).build();
        tmpChunkedLibrary.items = jsonArrayToSongArray(jellyfinPlaylistRequest(uri));
        return tmpChunkedLibrary;
    }

    public static Song getRandomSong() throws URISyntaxException, JellyfinHttpError, IOException, ParseException {
        URI uri = new URIBuilder().
                addParameter("ParentId", RADIO_PLAYLIST_ID).
                addParameter("SortBy", "Random").
                addParameter("IncludeItemTypes", "Audio").
                addParameter("Fields", "AudioInfo,BasicSyncInfo,Path,RuntimeTicks").
                addParameter("EnableTotalRecordCount", "false").
                addParameter("Limit", "1").build();
        return jsonArrayToSongArray(jellyfinItemsRequest(uri))[0];
    }

    public static Song getSongById(String id) throws IOException, ParseException, JellyfinHttpError {
        HttpGet get = new HttpGet(String.format("%s/Users/%s/Items/%s", BASE_URL, USER_ID, id));
        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() == 200) {
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
            return new Song(jsonObject);
        }
        throw new JellyfinHttpError(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
    }

    public static List<Song> search(String query) throws URISyntaxException, JellyfinHttpError, IOException, ParseException {
        List<Song> songList = new ArrayList<>();

        JSONArray songsAndAlbums = jellyfinItemsRequest(new URIBuilder().
                addParameter("searchTerm", query).
                addParameter("Limit", "24").
                addParameter("Fields", "AudioInfo,BasicSyncInfo,Path,RuntimeTicks").
                addParameter("Recursive", "true").
                addParameter("IncludeMedia", "true").
                addParameter("IncludeItemTypes", "Audio,MusicAlbum").build());
        for (Object o : songsAndAlbums) {
            JSONObject item = (JSONObject) o;
            if (item.get("Type").equals("Audio")) songList.add(new Song(item));
            else if (item.get("Type").equals("AudioAlbum")) {
                Song[] items = jsonArrayToSongArray(jellyfinItemsRequest(new URIBuilder().addParameter("ParentId", (String) item.get("Id")).build()));
                songList.addAll(Arrays.asList(items));
            }
        }

        return songList;
    }
}