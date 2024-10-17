package com.github.badbadbadbadbad.tsundoku.models;

/* Using Jikan.moe API */
/* https://jikan.moe/ */
/* docs.api.jikan.moe */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class AnimeAPIModel {

    // Jikan API
    private static final String BASE_URL = "https://api.jikan.moe/v4";

    public AnimeListInfo getCurrentSeason(int page) {
        try {
            String urlString = BASE_URL + "/seasons/now";
            urlString += "?page=" + String.valueOf(page);

            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("getCurrentSeason: HTTP Error Code " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            conn.disconnect();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response.toString());

            return parseAnimeData(rootNode);


        } catch (Exception e) {
            System.out.println("AnimeAPIModel getCurrentSeason() error: " + e);
            return null;
        }
    }


    public AnimeListInfo getTop(int page) {
        try {
            String urlString = BASE_URL + "/top/anime";
            urlString += "?page=" + String.valueOf(page);

            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("getTop: HTTP Error Code " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            conn.disconnect();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response.toString());

            return parseAnimeData(rootNode);

        } catch (Exception e) {
            System.out.println("AnimeAPIModel getTop() error: " + e);
            return null;
        }
    }

    // Jikan's interal search uses typesense.
    // This means that some special operators can be used, like - for exclusion and "" for exact search
    public AnimeListInfo getSearchByName(String query, int page) {
        try {
            String urlString = BASE_URL + "/anime";

            // Page
            urlString += "?page=" + String.valueOf(page);

            // Search query
            // urlString += "&q=" + "\"" + query + "\"";
            urlString += "&q=" + URLEncoder.encode("\"" + query + "\"", StandardCharsets.UTF_8);

            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("getSearchByName: HTTP Error Code " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            conn.disconnect();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response.toString());

            return parseAnimeData(rootNode);

        } catch (Exception e) {
            System.out.println("AnimeAPIModel getSearchByName() error: " + e);
            return null;
        }
    }


    private AnimeListInfo parseAnimeData(JsonNode animeData) {
        List<AnimeInfo> animeList = new ArrayList<>();
        JsonNode dataArray = animeData.get("data");

        if (dataArray.isArray()) {
            for (JsonNode animeNode : dataArray) {

                int id = animeNode.get("mal_id").asInt();
                String imageUrl = animeNode.get("images").get("jpg").get("large_image_url").asText();
                // String imageUrl = animeNode.get("images").get("jpg").get("image_url").asText();
                String publicationStatus = animeNode.get("status").asText();
                int episodesTotal = animeNode.get("episodes").asInt();
                String source = animeNode.get("source").asText();
                String synopsis = animeNode.get("synopsis").asText();

                String releaseSeason = animeNode.get("season").asText();
                int releaseYear = animeNode.get("year").asInt();
                String release;
                if (releaseSeason.equals("null") || releaseYear == 0) {
                    release = "Not yet provided";
                } else {
                    release = releaseSeason.substring(0, 1).toUpperCase() + releaseSeason.substring(1) + " " + releaseYear;
                }

                List<String> studioNames = new ArrayList<>();
                animeNode.get("studios").forEach(studio -> studioNames.add(studio.get("name").asText()));
                String studios = String.join(", ", studioNames);

                String ageRatingFull = animeNode.get("rating").asText();
                String ageRating = ageRatingFull
                        .replace("G - All Ages", "G")
                        .replace("PG - Children", "PG")
                        .replace("PG-13 - Teens 13 or older", "PG13")
                        .replace("R - 17+ (violence & profanity)", "R17+")
                        .replace("R+ - Mild Nudity", "R+")
                        .replace("Rx - Hentai", "Rx");

                String title = null;
                String titleJapanese = "None provided";
                String titleEnglish = "None provided";
                for (JsonNode titleNode : animeNode.get("titles")) {
                    String type = titleNode.get("type").asText();
                    String titleText = titleNode.get("title").asText();

                    if ("Default".equals(type)) {
                        title = titleText;
                    } else if ("Japanese".equals(type)) {
                        titleJapanese = titleText;
                    } else if ("English".equals(type)) {
                        titleEnglish = titleText;
                    }
                }


                AnimeInfo anime = new AnimeInfo(id, title, titleJapanese, titleEnglish, imageUrl, publicationStatus,
                        episodesTotal, source, ageRating, synopsis, release, studios);
                animeList.add(anime);
            }
        }

        int lastPage = animeData.get("pagination").get("last_visible_page").asInt();

        return new AnimeListInfo(removeDuplicates(animeList), lastPage);
    }

    private List<AnimeInfo> removeDuplicates(List<AnimeInfo> animeList) {
        Set<Integer> seenIds = new HashSet<>();
        return animeList.stream()
                .filter(anime -> seenIds.add(anime.getId()))
                .collect(Collectors.toList());
    }
}
