package com.marios8543;

import com.marios8543.discordbot.SongChangeListener;
import com.marios8543.musicsource.Song;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import spark.Service;

import java.time.Instant;
import java.util.*;

import static com.marios8543.Main.musicSource;
import static spark.Spark.halt;

public class RadioApi {
    private Song nowPlaying = null;
    private final List<Song> playlist = new ArrayList<>();
    private final List<Song> requests = new ArrayList<>();
    private final Map<String,Long> requestLimits = new HashMap<>();
    private static int currentTime = 0;
    private List<String> skipVotes = new ArrayList<>();
    private final int requestLimit = Integer.parseInt(System.getenv("REQUEST_LIMIT"));
    private boolean skip = false;
    public final WSHandler wsHandler = new WSHandler();
    private List<SongChangeListener> songChangeListeners = new ArrayList<>();
    public void addSongChangeListener(SongChangeListener listener) {
        songChangeListeners.add(listener);
    }

    RadioApi(Service server) {
        server.webSocket("/api/chat",wsHandler);
        server.get("/radio",(req,res)->{
            res.redirect("radio.html");
            return "";
        });

        server.get("/api/radio",(req,res)->{
            res.type("application/json; charset=utf-8");
            JSONObject ret = new JSONObject();
            ret.put("song",nowPlaying.toJSON());
            ret.put("time",currentTime);

            if (!req.cookies().containsKey("usernameId")) res.cookie("usernameId", (new Username()).getCookieString());

            JSONArray arr = new JSONArray();
            for (int i=0; i<Math.min(requests.size(), 5); i++) arr.add(requests.get(i).toJSON());
            for (int i=0; i<Math.min(5 - Math.min(requests.size(), 5), playlist.size()); i++) arr.add(playlist.get(i).toJSON());
            ret.put("upcoming",arr);
            return ret.toJSONString();
        });

        server.before("/api/radio/request",(request, response) -> {
            if (!(request.cookies().containsKey("admin_key") &&
                    request.cookies().get("admin_key").equals(System.getenv("ADMIN_KEY")))) {
                if (requestLimits.containsKey(request.ip())) {
                    long currentTime = Instant.now().getEpochSecond();
                    if ((currentTime-requestLimits.get(request.ip())) < requestLimit) {
                        halt(403,"You can only make a song request every "+requestLimit+" seconds");
                    }
                }
            }
        });

        server.get("/api/radio/request",(req,res) -> {
            Song song = musicSource.getSongById(req.queryParams("id")!=null ? req.queryParams("id") : "");
            JSONObject object = new JSONObject();
            requests.add(song);
            res.type("application/json; charset=utf-8");
            res.status(200);

            object.put("queue_position",requests.size());
            requestLimits.put(req.ip(),Instant.now().getEpochSecond());

            wsHandler.requested(req.ip(),song);
            return object.toJSONString();
        });
        
        server.before("/api/radio/voteSkip", (request, response) -> {
            if (skipVotes.contains(request.ip())) halt(403,"You've already voted to skip this song.");
            if (request.cookies().containsKey("admin_key") &&
                    request.cookies().get("admin_key").equals(System.getenv("ADMIN_KEY"))) {
                skip = true;
                halt();
            }
        });
        
        server.get("/api/radio/voteSkip",(req,res) -> {
            skipVotes.add(req.ip());
            int votesToSkip = switch (wsHandler.getListening()) {
                case 1 -> 1;
                case 2 -> 2;
                default -> (int) Math.ceil(wsHandler.getListening() / 2.0);
            };
            if (skipVotes.size()>=votesToSkip) {
                skip = true;
                return "Skipping";
            }
            else {
                JSONObject object = new JSONObject();
                object.put("votes",skipVotes.size());
                object.put("votesToSkip",votesToSkip);
                res.type("application/json; charset=utf-8");
                wsHandler.broadcast("skip_votes",String.format("%s/%s",skipVotes.size(),votesToSkip));
                return object.toJSONString();
            }
        });

        new Thread(() -> {
            while (true) {
                currentTime = 0;
                skipVotes = new ArrayList<>();
                if (requests.size()>0) {
                    nowPlaying = requests.get(0);
                    requests.remove(0);
                }
                else if (playlist.size() > 0){
                    nowPlaying = playlist.get(0);
                    playlist.remove(0);
                }
                if (skip) {
                    skip = false;
                    wsHandler.broadcast("force_reload","");
                }
                if (this.nowPlaying != null) {
                    songChangeListeners.forEach((listener -> listener.songChanged(this.nowPlaying)));
                    for(int i=0;i<nowPlaying.length;i++) {
                        currentTime++;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (skip) break;
                    }
                }
            }
        }).start();

        new Thread(() -> {
            while (true) {
                while (playlist.size() < 5) {
                    while (true) {
                        try {
                            Song song = musicSource.getRandomSong();
                            if (!song.title.toLowerCase().contains("inst") && !song.title.toLowerCase().contains("off vocal")) {
                                playlist.add(song);
                                break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println(e.getMessage() + "\nRetrying...");
                        }
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public Song getNowPlaying() {
        return nowPlaying;
    }

    public List<Song> getPlaylist() {
        return playlist;
    }

    public List<Song> getRequests() {
        return requests;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public void addRequest(Song song) {
        requests.add(song);
    }

}
