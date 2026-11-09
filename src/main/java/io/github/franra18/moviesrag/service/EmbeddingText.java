package io.github.franra18.moviesrag.service;

import io.github.franra18.moviesrag.model.Movie;
import java.util.ArrayList;
import java.util.List;

/**
 * Texts sent to gemini-embedding-2. Vectors are only comparable if every movie and every request
 * goes through here: changing the format means embedding all the movies again.
 */
public final class EmbeddingText {

	private EmbeddingText() {
	}

	public static String forMovie(Movie movie) {
		List<String> parts = new ArrayList<>();
		if (!movie.tagline().isBlank()) {
			parts.add(movie.tagline());
		}
		if (!movie.genres().isEmpty()) {
			parts.add("Genres: " + String.join(", ", movie.genres()) + ".");
		}
		if (!movie.keywords().isEmpty()) {
			parts.add("Keywords: " + String.join(", ", movie.keywords()) + ".");
		}
		parts.add(movie.overview());
		return singleLine("title: " + movie.title() + " | text: " + String.join(" ", parts));
	}

	public static String forQuery(String request) {
		return singleLine("task: search result | query: " + request);
	}

	private static String singleLine(String text) {
		return text.replaceAll("\\s+", " ").strip();
	}
}
