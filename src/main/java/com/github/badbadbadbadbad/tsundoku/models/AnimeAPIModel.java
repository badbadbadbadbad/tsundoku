package com.github.badbadbadbadbad.tsundoku.models;

/* Using Jikan.moe API (original data from MyAnimeList) */
/* https://jikan.moe/ */
/* docs.api.jikan.moe */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This module takes care of all requests for data of the Anime media type.
 * Requests are forwarded from here to the API we use, and the returned data is processed, then forwarded to the original caller.
 */
public class AnimeAPIModel {

    Clock utcClock = Clock.systemUTC();
    private static final String BASE_URL = "https://api.jikan.moe/v4";
    HttpClient client;

    private Map<String, Boolean> typeFilters;               // Types like TV, Movie, OVA..
    private Map<String, Boolean> ratingFilters;             // Age ratings like G, PG, R17+..
    private String orderBy;
    private String status;
    private String startYear;
    private String endYear;


    private String programName = "tsundoku";
    private String programVersion = "0.1-beta";
    private String os = System.getProperty("os.name");
    private String osVersion = System.getProperty("os.version");
    private String javaVersion = "21.0.4";                          // Statically bundled version of the program
    private String userAgent = programName + "/" + programVersion + " (Java " + javaVersion + ", " + os + " " + osVersion + ")";


    /**
     * API request for anime of the current season.
     * <p><a href="https://docs.api.jikan.moe/#tag/seasons/operation/getSeasonNow">Link to documentation</a></p>
     * @param page Jikan.moe API always returns (max) 25 items as a page. This specifies the page to be returned of the full data.
     * @return Result of API call, processed with parseAnimeData function.
     */
    public CompletableFuture<AnimeListInfo> getCurrentSeason(int page) {
        String urlString = BASE_URL + "/seasons/now?page=" + page;
        URI uri = URI.create(urlString);
        client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .orTimeout(5, TimeUnit.SECONDS)
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("getCurrentSeason(): HTTP Error Code " + response.statusCode());
                    }

                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode rootNode = mapper.readTree(response.body());
                        return parseAnimeData(rootNode);
                    } catch (IOException e) {
                        System.out.println("AnimeAPIModel getCurrentSeason() response parsing error: " + e.getMessage());
                        return new AnimeListInfo(Collections.emptyList(), 1);
                    }
                })
                .exceptionally(e -> {
                    System.out.println("AnimeAPIModel getCurrentSeason() error: " + e);
                    return new AnimeListInfo(Collections.emptyList(), 1);
                });
    }


    /**
     * API request for anime of upcoming seasons.
     * <p><a href="https://docs.api.jikan.moe/#tag/seasons/operation/getSeasonUpcoming">Link to documentation</a></p>
     * @param page Jikan.moe API always returns (max) 25 items as a page. This specifies the page to be returned of the full data.
     * @return Result of API call, processed with parseAnimeData function.
     */
    public CompletableFuture<AnimeListInfo> getUpcoming(int page) {
        String urlString = BASE_URL + "/seasons/upcoming?page=" + page;
        URI uri = URI.create(urlString);
        client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .orTimeout(5, TimeUnit.SECONDS)
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("getUpcoming(): HTTP Error Code " + response.statusCode());
                    }
                    // Handle the parsing and potential exceptions here
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode rootNode = mapper.readTree(response.body());
                        return parseAnimeData(rootNode);
                    } catch (IOException e) {
                        System.out.println("AnimeAPIModel getUpcoming() response parsing error: " + e.getMessage());
                        return new AnimeListInfo(Collections.emptyList(), 1);
                    }
                })
                .exceptionally(e -> {
                    System.out.println("AnimeAPIModel getUpcoming() error: " + e);
                    return new AnimeListInfo(Collections.emptyList(), 1);
                });
    }


    /**
     * API request for top anime of all time.
     * <p><a href="https://docs.api.jikan.moe/#tag/top/operation/getTopAnime">Link to documentation</a></p>
     * @param page Jikan.moe API always returns (max) 25 items as a page. This specifies the page to be returned of the full data.
     * @return Result of API call, processed with parseAnimeData function.
     */
    public CompletableFuture<AnimeListInfo> getTop(int page) {
        String urlString = BASE_URL + "/top/anime?page=" + page;
        URI uri = URI.create(urlString);
        client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .GET()
                .build();


        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .orTimeout(5, TimeUnit.SECONDS)
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("getTop: HTTP Error Code " + response.statusCode());
                    }
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode rootNode = mapper.readTree(response.body());
                        return parseAnimeData(rootNode);
                    } catch (IOException e) {
                        System.out.println("AnimeAPIModel getTop() response parsing error: " + e.getMessage());
                        return new AnimeListInfo(Collections.emptyList(), 1);
                    }
                })
                .exceptionally(e -> {
                    System.out.println("AnimeAPIModel getTop() error: " + e);
                    return new AnimeListInfo(Collections.emptyList(), 1);
                });
    }


    /**
     * API request for anime depending on a search string.
     * <p><a href="https://docs.api.jikan.moe/#tag/anime/operation/getAnimeSearch">Link to documentation</a></p>
     * @param query The search string to be used in the request.
     * @param page Jikan.moe API always returns (max) 25 items as a page. This specifies the page to be returned of the full data.
     * @return Result of API call, processed with parseAnimeData function.
     */
    public CompletableFuture<AnimeListInfo> getSearchByName(String query, int page) {
        String urlString = BASE_URL + "/anime?page=" + page + "&q=" + URLEncoder.encode("\"" + query + "\"", StandardCharsets.UTF_8);
        urlString += decodeOrderBy() + decodeStatus() + decodeStartYear() + decodeEndYear(); // Order and filters for search query

        
        URI uri = URI.create(urlString);
        client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .GET()
                .build();


        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .orTimeout(5, TimeUnit.SECONDS)
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("getSearchByName: HTTP Error Code " + response.statusCode());
                    }

                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode rootNode = mapper.readTree(response.body());
                        return parseAnimeData(rootNode);
                    } catch (IOException e) {
                        System.out.println("AnimeAPIModel getSearchByName() response parsing error: " + e.getMessage());
                        return new AnimeListInfo(Collections.emptyList(), 1);
                    }
                })
                .exceptionally(e -> {
                    System.out.println("AnimeAPIModel getSearchByName() error: " + e);
                    return new AnimeListInfo(Collections.emptyList(), 1);
                });
    }


    /**
     * API request for a specific anime depending on its MyAnimeList ID.
     * <p><a href="https://docs.api.jikan.moe/#tag/anime/operation/getAnimeById">Link to documentation</a></p>
     * @param id The ID to be used for the request. As Jikan.Moe uses MyAnimeList data, they also use their ID system.
     * @return Result of API call, processed with parseSingleAnimeData function. Returns null on a bad API call.
     */
    public CompletableFuture<AnimeInfo> getAnimeByID(int id) {
        String urlString = BASE_URL + "/anime/" + id;
        URI uri = URI.create(urlString);
        client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .GET()
                .build();


        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .orTimeout(5, TimeUnit.SECONDS)
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("getAnimeByID: HTTP Error Code " + response.statusCode());
                    }
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode rootNode = mapper.readTree(response.body());
                        return parseSingleAnimeData(rootNode);
                    } catch (IOException e) {
                        System.out.println("AnimeAPIModel getAnimeByID() response parsing error: " + e.getMessage());
                        return null;
                    }
                })
                .exceptionally(e -> {
                    System.out.println("AnimeAPIModel getAnimeByID() error: " + e);
                    return null;
                });
    }


    /**
     * Processes results of an API call from JSON tree form to something more usable.
     * Data for every anime element is parsed and stored in a AnimeInfo container object.
     * The full list of AnimeInfo objects are stored in an AnimeListInfo object, together with the page number of the original request.
     * @param node The JSON tree full of anime data returned by an API request.
     * @return An AnimeListInfo object, containing a List of processed anime data in AnimeInfo form, together with the page number of the request.
     */
    private AnimeListInfo parseAnimeData(JsonNode node) {
        List<AnimeInfo> animeList = new ArrayList<>();
        JsonNode dataArray = node.get("data");

        if (dataArray.isArray()) {
            for (JsonNode animeNode : dataArray) {

                // ID, guaranteed to be an existing integer
                int id = animeNode.get("mal_id").asInt();

                // Image URL, String or null
                String imageUrl = animeNode.get("images").get("jpg").get("large_image_url").asText();
                if (imageUrl.equals("null"))
                    imageUrl = "Not yet provided";

                // Small image URL, String or null
                String smallImageUrl = animeNode.get("images").get("jpg").get("image_url").asText();
                if (smallImageUrl.equals("null"))
                    smallImageUrl = "Not yet provided";

                // Publication status, enum of "Finished Airing", "Currently Airing", "Not yet aired" or null
                String publicationStatusFull = animeNode.get("status").asText();
                String publicationStatus = (publicationStatusFull.equals("null")) ? "Not yet provided" :
                        publicationStatusFull.replace("Finished Airing", "Complete")
                                .replace("Currently Airing", "Airing")
                                .replace("Not yet aired", "Upcoming");

                // Source, String or null
                String source = animeNode.get("source").asText();
                if (source.equals("null"))
                    source = "Not yet provided";

                // Synopsis, String or null
                String synopsis = animeNode.get("synopsis").asText();
                if (synopsis.equals("null"))
                    synopsis = "Not yet provided";

                // Type of anime, enum of "TV", "Movie", "OVA", "Special", "ONA", "Music", "CM", "PV", "TV Special" or null
                String animeType = animeNode.get("type").asText();
                if (animeType.equals("null"))
                    animeType = "Not yet provided";

                // Release season + year. MAL is bad at filling the data in here, so we have to attempt to calculate it from a few fields.
                // Ends up as a "<Season> <Year>" string, like "Winter 2023", or "Not yet provided"
                String releaseSeason = animeNode.get("season").asText();
                int releaseYear = animeNode.get("year").asInt();
                int releaseMonthFromProp = animeNode.get("aired").get("prop").get("from").get("month").asInt();
                int releaseYearFromProp = animeNode.get("aired").get("prop").get("from").get("year").asInt();
                String release = calculateReleaseSeason(releaseSeason, releaseYear, releaseMonthFromProp, releaseYearFromProp);

                // Total amount of episodes. "1" for movies. This is 0 if not yet known, which we keep as a "not yet known" case.
                int episodesTotal = animeNode.get("episodes").asInt();
                // if (episodesTotal == 0)
                    // episodesTotal = 1;

                // List of studios involved in anime creation, we comma-seperate it.
                // Not sure if object can be empty, so if no studios are provided, set to "Not yet provided".
                List<String> studioNames = new ArrayList<>();
                animeNode.get("studios").forEach(studio -> studioNames.add(studio.get("name").asText()));
                String studios = studioNames.isEmpty() ? "Not yet provided" : String.join(", ", studioNames);

                // Age rating. Wordy enum of values, so we shorten them. Can also be null, which we turn to "Not yet provided".
                String ageRatingFull = animeNode.get("rating").asText();
                String ageRating = (ageRatingFull.equals("null")) ? "Not yet provided" :
                        ageRatingFull.replace("G - All Ages", "G")
                        .replace("PG - Children", "PG")
                        .replace("PG-13 - Teens 13 or older", "PG13")
                        .replace("R - 17+ (violence & profanity)", "R17+")
                        .replace("R+ - Mild Nudity", "R+")
                        .replace("Rx - Hentai", "Rx");

                // Titles. Note that JP and EN titles may not exist, hence the default case for them.
                // The "normal" title (Roumaji in MAL) _should_ always exist, but we still provide a dumb base case because Jikan API is unclear.
                String title = "No title provided";
                String titleJapanese = "Not yet provided";
                String titleEnglish = "Not yet provided";
                for (JsonNode titleNode : animeNode.get("titles")) {
                    String titleType = titleNode.get("type").asText();
                    String titleText = titleNode.get("title").asText();

                    if (titleType.equals("Default")) {
                        title = titleText;
                    } else if (titleType.equals("Japanese")) {
                        titleJapanese = titleText;
                    } else if (titleType.equals("English")) {
                        titleEnglish = titleText;
                    }
                }

                // Current date in UTC for lastUpdated timestamp
                LocalDate currentDate = LocalDate.now(utcClock);
                String dateString = currentDate.toString();

                AnimeInfo anime = new AnimeInfo(id, title, titleJapanese, titleEnglish, imageUrl, smallImageUrl,
                        publicationStatus, episodesTotal, source, ageRating, synopsis, release, studios, animeType, dateString);
                animeList.add(anime);
            }
        }

        int lastPage = node.get("pagination").get("last_visible_page").asInt();

        List<AnimeInfo> filteredAnimeList = filterByTypeAndRating(animeList);
        return new AnimeListInfo(removeDuplicates(filteredAnimeList), lastPage);
    }


    /**
     * The same functionality as parseAnimeData, but adjusted to a single anime instead of a List.
     * @param node The JSON tree containing data on a single anime returned by an API request.
     * @return An AnimeInfo object containing the processed anime data.
     */
    private AnimeInfo parseSingleAnimeData(JsonNode node) {
        JsonNode animeNode = node.get("data");

        // ID, guaranteed to be an existing integer
        int id = animeNode.get("mal_id").asInt();

        // Image URL, String or null
        String imageUrl = animeNode.get("images").get("jpg").get("large_image_url").asText();
        if (imageUrl.equals("null"))
            imageUrl = "Not yet provided";

        // Small image URL, String or null
        String smallImageUrl = animeNode.get("images").get("jpg").get("image_url").asText();
        if (smallImageUrl.equals("null"))
            smallImageUrl = "Not yet provided";

        // Publication status, enum of "Finished Airing", "Currently Airing", "Not yet aired" or null
        String publicationStatusFull = animeNode.get("status").asText();
        String publicationStatus = (publicationStatusFull.equals("null")) ? "Not yet provided" :
                publicationStatusFull.replace("Finished Airing", "Complete")
                        .replace("Currently Airing", "Airing")
                        .replace("Not yet aired", "Upcoming");

        // Source, String or null
        String source = animeNode.get("source").asText();
        if (source.equals("null"))
            source = "Not yet provided";

        // Synopsis, String or null
        String synopsis = animeNode.get("synopsis").asText();
        if (synopsis.equals("null"))
            synopsis = "Not yet provided";

        // Type of anime, enum of "TV", "Movie", etc., or null
        String animeType = animeNode.get("type").asText();
        if (animeType.equals("null"))
            animeType = "Not yet provided";

        // Release season + year
        String releaseSeason = animeNode.get("season").asText();
        int releaseYear = animeNode.get("year").asInt();
        int releaseMonthFromProp = animeNode.get("aired").get("prop").get("from").get("month").asInt();
        int releaseYearFromProp = animeNode.get("aired").get("prop").get("from").get("year").asInt();
        String release = calculateReleaseSeason(releaseSeason, releaseYear, releaseMonthFromProp, releaseYearFromProp);

        // Total episodes
        int episodesTotal = animeNode.get("episodes").asInt();
        // if (episodesTotal == 0)
        //     episodesTotal = 1;

        // Studios involved in anime creation
        List<String> studioNames = new ArrayList<>();
        animeNode.get("studios").forEach(studio -> studioNames.add(studio.get("name").asText()));
        String studios = studioNames.isEmpty() ? "Not yet provided" : String.join(", ", studioNames);

        // Age rating
        String ageRatingFull = animeNode.get("rating").asText();
        String ageRating = (ageRatingFull.equals("null")) ? "Not yet provided" :
                ageRatingFull.replace("G - All Ages", "G")
                        .replace("PG - Children", "PG")
                        .replace("PG-13 - Teens 13 or older", "PG13")
                        .replace("R - 17+ (violence & profanity)", "R17+")
                        .replace("R+ - Mild Nudity", "R+")
                        .replace("Rx - Hentai", "Rx");

        // Titles
        String title = "No title provided";
        String titleJapanese = "Not yet provided";
        String titleEnglish = "Not yet provided";
        for (JsonNode titleNode : animeNode.get("titles")) {
            String titleType = titleNode.get("type").asText();
            String titleText = titleNode.get("title").asText();

            if (titleType.equals("Default")) {
                title = titleText;
            } else if (titleType.equals("Japanese")) {
                titleJapanese = titleText;
            } else if (titleType.equals("English")) {
                titleEnglish = titleText;
            }
        }

        // Current date in UTC for lastUpdated timestamp
        LocalDate currentDate = LocalDate.now(utcClock);
        String dateString = currentDate.toString();

        return new AnimeInfo(id, title, titleJapanese, titleEnglish, imageUrl, smallImageUrl, publicationStatus,
                episodesTotal, source, ageRating, synopsis, release, studios, animeType, dateString);
    }


    /**
     * Attempts to calculate a release date String.
     * MyAnimeList stores dates in strange formats in multiple ways, so if the main date field is not provided, we use the alternate date data fields.
     * @param releaseSeason The main data field for release season used by MyAnimeList.
     * @param releaseYear The main data field for release year used by MyAnimeList.
     * @param releaseMonthFromProp Alternative data field for release month used by MyAnimeList.
     * @param releaseYearFromProp Alternative data field for release year used by MyAnimeList.
     * @return A String for the release date in the form of "Season + Year", if possible. Else, "Not yet provided".
     */
    // MAL data can be pretty decroded. This attempts to use provided release timings to get a usable "Season + Year" string from it.
    private String calculateReleaseSeason(String releaseSeason, int releaseYear, int releaseMonthFromProp, int releaseYearFromProp) {
        String release;

        // If the releaseSeason and releaseYear are provided, just use them
        if (!(releaseSeason.equals("null")) && !(releaseYear == 0)) {
            release = releaseSeason.substring(0, 1).toUpperCase() + releaseSeason.substring(1) + " " + releaseYear;
        }

        // If they are not provided, the "aired" field sometimes contains month and year of airing
        else if (!(releaseMonthFromProp == 0) && !(releaseYearFromProp == 0)) {

            // This is _generally_ how seasons are split by months.
            if (releaseMonthFromProp < 4) {
                releaseSeason = "Winter";
            } else if (releaseMonthFromProp < 7) {
                releaseSeason = "Spring";
            } else if (releaseMonthFromProp < 10) {
                releaseSeason = "Summer";
            } else {
                releaseSeason = "Fall";
            }

            release = releaseSeason + " " + releaseYearFromProp;
        }

        // If both methods fail, then we just accept the data sucks here
        else {
            release = "Not yet provided";
        }

        return release;
    }


    /**
     * Turns the "Order by" filter String from the View element into URL form for API requests.
     * @return The converted String.
     */
    private String decodeOrderBy() {
        return switch (orderBy) {
            case "Title: Ascending" -> "&order_by=title&sort=asc";
            case "Title: Descending" -> "&order_by=title&sort=desc";
            case "Rating: Highest" -> "&order_by=score&sort=desc";
            case "Rating: Lowest" -> "&order_by=score&sort=asc";
            case "Popular: Least" -> "&order_by=popularity&sort=desc"; // Popularity is a rank in MAL, so "1" is most popular
            case "Popular: Most" -> "&order_by=popularity&sort=asc";
            default ->  // Default case: No ordering. Jikan API does not document this, but produces fine results.
                    "";
        };
    }

    /**
     * Turns the "Status" filter String from the View element into URL form for API requests.
     * @return The converted String.
     */
    private String decodeStatus() {
        if (status.equals("Any")) {
            return "";
        } else {
            return "&status=" + status.toLowerCase();
        }
    }


    /**
     * Turns the "Start year" filter String from the View element into URL form for API requests.
     * @return The converted String.
     */
    private String decodeStartYear() {
        if (startYear.isEmpty()) {
            return "";
        } else {
            return "&start_date=" + String.format("%04d", Integer.parseInt(startYear)) + "-01-01";
        }
    }


    /**
     * Turns the "End year" filter String from the View element into URL form for API requests.
     * @return The converted String.
     */
    private String decodeEndYear() {
        if (endYear.isEmpty()) {
            return "";
        } else {
            return "&end_date=" + String.format("%04d", Integer.parseInt(endYear)) + "-12-31";
        }
    }


    /**
     * The Jikan.Moe API produces duplicates in some cases due to some issues on their backend.
     * There are multiple bug reports on this behaviour, for example <a href="https://github.com/jikan-me/jikan-rest/issues/262">here</a>.
     * This function is always called during parsing of data returned by the API to filter out such duplicates.
     * @param animeList A List of AnimeInfo objects, created by parseAnimeData().
     * @return The same List, with duplicates filtered from the List.
     */
    private List<AnimeInfo> removeDuplicates(List<AnimeInfo> animeList) {
        Set<Integer> seenIds = new HashSet<>();
        return animeList.stream()
                .filter(anime -> seenIds.add(anime.getId()))
                .collect(Collectors.toList());
    }


    /**
     * The Jikan.Moe API provides filters for age and type ratings in requests, but multi-selections are not possible.
     * Thus, we always call for unfiltered data on their end, then filter the data on our end.
     * The filter options themselves are provided by the user via the SettingsView.
     * @param animeList A List of AnimeInfo objects, created by parseAnimeData().
     * @return The same List, with anime of unwanted age / type ratings removed from the List.
     */
    private List<AnimeInfo> filterByTypeAndRating(List<AnimeInfo> animeList) {
        return animeList.stream()
                .filter(anime -> typeFilters.getOrDefault(anime.getType(), false))
                .filter(anime -> ratingFilters.getOrDefault(anime.getAgeRating(), false))
                .collect(Collectors.toList());
    }


    public void setTypeFilters(Map<String, Boolean> typeFilters) {
        this.typeFilters = typeFilters;
    }


    public void setRatingFilters(Map<String, Boolean> ratingFilters) {
        this.ratingFilters = ratingFilters;
    }


    public void setSearchFilters(String orderBy, String status, String startYear, String endYear) {
        this.orderBy = orderBy;
        this.status = status;
        this.startYear = startYear;
        this.endYear = endYear;
    }

}
