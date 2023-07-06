package com.marios8543;

import com.marios8543.discordbot.DiscordBot;
import spark.Service;

import static spark.Service.ignite;
import static spark.Spark.exception;
import static spark.Spark.internalServerError;


/** @noinspection unchecked*/
class Main {
    private static final Service server = ignite().port(4567);
    private static RadioApi radioContext;

    public static void main(String[] args) {
        server.staticFiles.location("/public");
        radioContext = new RadioApi(server);
        new PlayerApi(server);
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
