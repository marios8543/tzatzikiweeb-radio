package com.marios8543;

import com.marios8543.musicsource.ChunkedLibrary;
import com.marios8543.musicsource.Song;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import spark.Service;

import static com.marios8543.Main.musicSource;

class PlayerApi {
    public PlayerApi(Service server){

        server.get("/api/player",(req,res)->{
            int offset = Integer.parseInt(req.queryParams("s")!=null ? req.queryParams("s") : "0");
            String sortBy = req.queryParams("sortBy");
            String query = req.queryParams("q");

            JSONObject ret = new JSONObject();
            JSONArray retarr = new JSONArray();

            ChunkedLibrary chunkedLibrary = musicSource.search(query, sortBy, offset);
            for (Song s : chunkedLibrary.items) retarr.add(s.toJSON());
            ret.put("songs",retarr);
            ret.put("offset_count", chunkedLibrary.totalCount / 30);

            res.type("application/json; charset=utf-8");
            return ret.toJSONString();
        });

        server.get("/api/cover", (req, res) -> {
            String id = req.queryParams("id");
            return musicSource.getCoverImage(id);
        });
    }
}
