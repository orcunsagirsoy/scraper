package com.example.spring_boot;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.concurrent.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


@RestController
public class HelloController  {

    @GetMapping("/")
    public void index() throws InterruptedException, ExecutionException {


        // List of financial news RSS feeds
        List<String> rssFeeds = Arrays.asList(
                "https://www.ft.com/rss/home",
                "https://www.forbes.com/investing/feed/",
                "https://www.cnbc.com/id/100003114/device/rss/rss.html",
                "https://finance.yahoo.com/rss/",
                "https://www.investopedia.com/feedbuilder/feed/getfeed?feedName=rss_articles",
                "https://www.reutersagency.com/en/reutersbest/reuters-best-rss-feeds/",
                "https://www.marketwatch.com/rss/",
                "https://www.wsj.com/news/rss-news-and-feeds",
                "https://feeds.bloomberg.com/markets/news.rss",
                "https://money.cnn.com/services/rss/",
                "https://www.economist.com/rss",
                "https://www.businessinsider.com/rss",
                "https://www.morningstar.com/rss/rss.aspx",
                "https://www.npr.org/rss/rss.php?id=1017",
                "https://www.fool.com/investing-news/feed.xml"
        );

        // Keywords to search for in news titles
        List<String> keywords = Arrays.asList("stock", "market", "investment", "finance", "economy", "trading", "earnings", "inflation");

        // Shared data structure to store results (thread-safe)
        Map<String, List<Map<String, String>>> newsByFeed = new ConcurrentHashMap<>();

        // Process feeds concurrently using virtual threads
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Map.Entry<String, List<Map<String, String>>>>> futures = rssFeeds.stream()
                    .map(feed -> executor.submit(() -> scrapeRssFeedDetailed(feed, keywords)))
                    .toList();

            // Collect results
            for (Future<Map.Entry<String, List<Map<String, String>>>> future : futures) {
                Map.Entry<String, List<Map<String, String>>> result = future.get();
                newsByFeed.put(result.getKey(), result.getValue());
            }
        }

        // Print the results
        System.out.println("Scraped News Details:");
        newsByFeed.forEach((feed, articles) -> {
            System.out.println("From Feed: " + feed);
            for (Map<String, String> article : articles) {
                System.out.println("  Title: " + article.get("title"));
                System.out.println("  Link: " + article.get("link"));
                System.out.println("  Description: " + article.get("description"));
                System.out.println("  Publication Date: " + article.get("pubDate"));
                System.out.println();
            }
        });
    }

    // Function to scrape an RSS feed for news content with details
    private static Map.Entry<String, List<Map<String, String>>> scrapeRssFeedDetailed(String rssFeed, List<String> keywords) {
        List<Map<String, String>> matchedNews = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(rssFeed).get();
            Elements items = doc.select("item");

            for (Element item : items) {
                String title = item.select("title").text();
                String description = item.select("description").text();
                String pubDate = item.select("pubDate").text();
                String link = item.select("link").text();

                // Check if keywords match in title or description
                boolean matches = keywords.stream().anyMatch(keyword ->
                        title.toLowerCase().contains(keyword) || description.toLowerCase().contains(keyword));

                if (matches) {
                    Map<String, String> articleDetails = new HashMap<>();
                    articleDetails.put("title", title);
                    articleDetails.put("description", description);
                    articleDetails.put("pubDate", pubDate);
                    articleDetails.put("link", link);
                    matchedNews.add(articleDetails);
                }
            }
        } catch (Exception e) {
            System.err.println("Error scraping feed: " + rssFeed + " - " + e.getMessage());
        }

        return Map.entry(rssFeed, matchedNews);
    }
}
