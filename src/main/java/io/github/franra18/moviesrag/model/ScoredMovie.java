package io.github.franra18.moviesrag.model;

/**
 * A search result.
 *
 * @param score cosine similarity between the request and the movie, from -1 to 1
 */
public record ScoredMovie(Movie movie, double score) {
}
