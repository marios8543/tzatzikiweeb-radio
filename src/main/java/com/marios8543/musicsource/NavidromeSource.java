package com.marios8543.musicsource;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class NavidromeSource implements MusicSource {
    private final String username;
    private final String password;
    private final String host;

    public static final int MAX_LIBRARY_SONGS = 20;

    private final HttpClient httpClient = HttpClientBuilder.create().build();

    public NavidromeSource(String url, String username, String password) {
        this.username = username;
        this.password = password;
        this.host = url;
    }

    private HttpResponse subsonicBaseRequest(String url, List<NameValuePair> uriParams) throws URISyntaxException, IOException, ParseException {
        URIBuilder uriBuilder = new URIBuilder(host + "/rest/" + url)
                .addParameter("u", username)
                .addParameter("p", password)
                .addParameter("v", "1.12.0")
                .addParameter("c", "Radio")
                .addParameter("f", "json");
        if (uriParams != null)
            uriBuilder.addParameters(uriParams);
        URI uri = uriBuilder.build();

        HttpGet get = new HttpGet(uri);
        return httpClient.execute(get);
    }

    private JSONObject subsonicRequest(String url, List<NameValuePair> uriParams) throws URISyntaxException, IOException, ParseException {
        HttpResponse response = subsonicBaseRequest(url, uriParams);
        JSONObject object = (JSONObject) new JSONParser().parse(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
        return (JSONObject) object.get("subsonic-response");
    }

    private JSONObject subsonicRequest(String url) throws URISyntaxException, IOException, ParseException {
        return subsonicRequest(url, null);
    }

    public InputStream getCoverImage(String id) throws MusicSourceException {
        try {
            HttpResponse response = subsonicBaseRequest("getCoverArt", List.of(new BasicNameValuePair("id", id)));
            if (response.getStatusLine().getStatusCode() != 200)
                throw new MusicSourceException(new Exception("Song ID not found"));
            return response.getEntity().getContent();
        } catch (Exception e) {
            throw new MusicSourceException(e);
        }
    }

    @Override
    public ChunkedLibrary search(String query, String sort, int offset) throws MusicSourceException {
        try {
            JSONObject response = subsonicRequest("search2", List.of(
                    new BasicNameValuePair("query", query),
                    new BasicNameValuePair("artistCount", "0"),
                    new BasicNameValuePair("albumCount", "0"),
                    new BasicNameValuePair("orderBy", sort),
                    new BasicNameValuePair("songOffset", String.format("%d", offset * MAX_LIBRARY_SONGS)),
                    new BasicNameValuePair("songCount", String.format("%d", MAX_LIBRARY_SONGS))
            ));
            JSONArray jsonSongs = (JSONArray) ((JSONObject)response.get("searchResult2")).get("song");
            Song[] songs = (Song[]) jsonSongs.stream().map((obj) -> new Song((JSONObject) obj)).toArray(Song[]::new);
            int totalCount = Math.toIntExact((long)((JSONObject) subsonicRequest("getScanStatus").get("scanStatus")).get("count"));
            return new ChunkedLibrary(songs, totalCount);
        } catch (Exception e) {
            throw new MusicSourceException(e);
        }
    }

    @Override
    public ChunkedLibrary search(String query) throws MusicSourceException {
        return search(query, "ALBUM", 0);
    }

    @Override
    public Song getSongById(String id) throws MusicSourceException {
        try {
            JSONObject response = subsonicRequest("getSong", List.of(new BasicNameValuePair("id", id)));
            return new Song((JSONObject) response.get("song"));
        } catch (Exception e) {
            throw new MusicSourceException(e);
        }
    }

    @Override
    public Song getRandomSong() throws MusicSourceException {
        try {
            JSONObject response = subsonicRequest("getRandomSongs");
            JSONObject song = (JSONObject) ((JSONArray)((JSONObject) response.get("randomSongs")).get("song")).get(0);
            return new Song(song);
        } catch (Exception e) {
            throw new MusicSourceException(e);
        }
    }

    @Override
    public ChunkedLibrary getLibrary(int offset, String sort) throws MusicSourceException {
        return search("", sort, offset);
    }
}
