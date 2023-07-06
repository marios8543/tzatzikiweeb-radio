package com.marios8543;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import spark.Service;

class PlayerApi {
    public PlayerApi(Service server){

        server.get("/api/player",(req,res)->{
            int offset = Integer.parseInt(req.queryParams("s")!=null ? req.queryParams("s") : "0");
            String sortBy = req.queryParams("sortBy");

            JSONObject ret = new JSONObject();
            JSONArray retarr = new JSONArray();

            JellyfinClient.ChunkedLibrary chunkedLibrary = JellyfinClient.getLibrary(offset, sortBy);
            for (Song s : chunkedLibrary.items) retarr.add(s.toJSON());
            ret.put("songs",retarr);
            ret.put("offset_count", chunkedLibrary.totalCount / 30);

            res.type("application/json; charset=utf-8");
            return ret.toJSONString();
        });

        server.get("/api/search",(req,res)->{
            JSONObject ret = new JSONObject();
            res.type("application/json; charset=utf-8");
            String query = req.queryParams("q");
            if(query==null || query.length()<3){
                res.status(400);
                ret.put("message","No search query or query shorter than 3 characters");
            }
            else {
                res.status(200);
                query = query.toLowerCase();
                JSONArray retarr = new JSONArray();
                for (Song s : JellyfinClient.search(query)) retarr.add(s.toJSON());
                ret.put("songs",retarr);
            }
            return ret.toJSONString();
        });
    }
}
