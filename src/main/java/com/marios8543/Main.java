package com.marios8543;

import com.marios8543.discordbot.DiscordBot;
import com.marios8543.musicsource.NavidromeSource;
import spark.Service;

import static spark.Service.ignite;
import static spark.Spark.exception;
import static spark.Spark.internalServerError;


/** @noinspection unchecked*/
public class Main {
    private static final Service server = ignite().port(4567);

    public static final NavidromeSource musicSource = new NavidromeSource(
            System.getenv("ND_HOST"),
            System.getenv("ND_USER"),
            System.getenv("ND_PASS")
    );

    public static void main(String[] args) {
        server.staticFiles.location("/public");
        RadioApi radioContext = new RadioApi(server);
        new PlayerApi(server);
        String discordToken = System.getenv("DISCORD_TOKEN");
        if (discordToken != null)
            new DiscordBot(System.getenv("DISCORD_TOKEN"), radioContext);

        internalServerError((req, res) -> {
            res.type("application/json");
            return "{\"message\":\"Internal server error\"}";
        });

        exception(Exception.class,(exception,request,response)->{
            response.type("application/json");
            response.status(500);
            response.body(String.format("{\"message\":\"%s\"}",exception.getMessage()));
            exception.printStackTrace();
        });

        System.out.println("Starting server");
        server.get("/radio",(req,res)->{
            res.redirect("radio.html");
            return "";
        });
    }
}
