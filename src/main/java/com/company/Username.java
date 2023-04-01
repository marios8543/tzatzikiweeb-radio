package com.company;

import org.eclipse.jetty.websocket.api.Session;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public class Username {
    public String username;
    public int adjectiveIdx;
    public int nounIdx;
    private static final Random random = new Random();

    public Username(Session session) {
        this();
        try {
            Optional<HttpCookie> cookieSearchResult = session.getUpgradeRequest().getCookies().stream().filter(httpCookie -> httpCookie.getName().equals("usernameId")).findFirst();
            if (cookieSearchResult.isPresent()) {
                String usernameId = cookieSearchResult.get().getValue();
                nounIdx = Integer.parseInt(usernameId.split("-")[0]);
                adjectiveIdx = Integer.parseInt(usernameId.split("-")[1]);

                username = formatUsername(adjectives.get(adjectiveIdx).toString(), nouns.get(nounIdx).toString());
            }
            else throw new Exception("Internal Flow Exception");
        }
        catch (Exception ignored) {
        }
    }

    public Username() {
        int[] res = getUsername();
        adjectiveIdx = res[0];
        nounIdx = res[1];
        username = formatUsername(adjectives.get(adjectiveIdx).toString(), nouns.get(nounIdx).toString());
    }

    public String getCookieString() {
        return String.format("%d-%d", nounIdx, adjectiveIdx);
    }

    private static void  populateArrays() {
        if (nouns != null && adjectives != null) return;
        JSONParser parser = new JSONParser();
        try {
            nouns = (JSONArray) parser.parse(new InputStreamReader(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("nouns-list.json"))));
            adjectives = (JSONArray) parser.parse(new InputStreamReader(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("adjectives-list.json"))));
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static String formatUsername(String adjective, String noun) {
        adjective = adjective.substring(0, 1).toUpperCase() + adjective.substring(1);
        noun = noun.substring(0, 1).toUpperCase() + noun.substring(1);
        return String.format("%s%s",adjective,noun);
    }

    private static final String list_location = System.getenv("list_location");
    private static JSONArray nouns;
    private static JSONArray adjectives;

    private static int[] getUsername() {
        populateArrays();
        int adjective = random.nextInt(adjectives.size()-1);
        int noun = random.nextInt(nouns.size()-1);
        return new int[]{adjective, noun};
    }
}