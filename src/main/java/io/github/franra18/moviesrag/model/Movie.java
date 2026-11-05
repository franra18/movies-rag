package io.github.franra18.moviesrag.model;

import java.util.List;

/**
 * One row of data/movies.csv.
 *
 * @param year    null when the release date is unknown
 * @param tagline empty when the movie has none
 * @param runtime minutes; null when unknown (TMDB uses 0 for that)
 */
public record Movie(
		int id,
		String title,
		Integer year,
		String overview,
		String tagline,
		List<String> genres,
		List<String> keywords,
		Integer runtime,
		double voteAverage,
		int voteCount,
		String originalLanguage) {

	public Movie {
		tagline = tagline == null ? "" : tagline;
		genres = genres == null ? List.of() : List.copyOf(genres);
		keywords = keywords == null ? List.of() : List.copyOf(keywords);
		if (runtime != null && runtime <= 0) {
			runtime = null;
		}
	}
}
