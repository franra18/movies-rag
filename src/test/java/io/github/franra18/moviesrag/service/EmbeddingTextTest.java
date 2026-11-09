package io.github.franra18.moviesrag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.franra18.moviesrag.model.Movie;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmbeddingTextTest {

	private static Movie movie(String overview, String tagline, List<String> genres, List<String> keywords) {
		return new Movie(1, "Four Rooms", 1995, overview, tagline, genres, keywords, 98, 6.5, 530, "en");
	}

	@Test
	void movieTextHasEveryPartInOrder() {
		Movie movie = movie("A bellhop's first night.", "Twelve guests.", List.of("Crime", "Comedy"), List.of("hotel", "bet"));

		assertEquals(
				"title: Four Rooms | text: Twelve guests. Genres: Crime, Comedy. Keywords: hotel, bet. A bellhop's first night.",
				EmbeddingText.forMovie(movie));
	}

	@Test
	void emptyPartsAreLeftOut() {
		Movie movie = movie("A bellhop's first night.", "", List.of(), List.of());

		assertEquals("title: Four Rooms | text: A bellhop's first night.", EmbeddingText.forMovie(movie));
	}

	@Test
	void lineBreaksBecomeSpaces() {
		Movie movie = movie("A witch, a hunter\nand a warlock.", "", List.of(), List.of());

		assertEquals("title: Four Rooms | text: A witch, a hunter and a warlock.", EmbeddingText.forMovie(movie));
	}

	@Test
	void queryGetsThePrefix() {
		assertEquals("task: search result | query: a sad movie for a rainy day",
				EmbeddingText.forQuery("  a sad movie\nfor a rainy day "));
	}
}
