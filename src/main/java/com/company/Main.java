package com.company;

import org.json.simple.parser.ParseException;
import spark.Service;

import java.io.IOException;
import java.net.URISyntaxException;

import static spark.Service.ignite;
import static spark.Spark.exception;
import static spark.Spark.internalServerError;


/** @noinspection unchecked*/
class Main {
    private static final Service server = ignite().port(4567);

    public static void main(String[] args) {
        server.staticFiles.location("/public");

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

        new RadioApi(server);
        new PlayerApi(server);
        server.get("/radio",(req,res)->{
            res.redirect("radio.html");
            return "";
        });
    }
}
