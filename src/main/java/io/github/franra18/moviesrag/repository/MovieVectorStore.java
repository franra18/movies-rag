package io.github.franra18.moviesrag.repository;

import io.github.franra18.moviesrag.MoviesRagProperties;
import io.github.franra18.moviesrag.model.Movie;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/** Every movie that has a vector, loaded once at startup. */
@Repository
@Profile("!ingest")
public class MovieVectorStore {

	private static final Logger log = LoggerFactory.getLogger(MovieVectorStore.class);

	public record Entry(Movie movie, float[] vector) {
	}

	private final List<Entry> entries;

	public MovieVectorStore(MovieCsvReader movieReader, EmbeddingsFile embeddingsFile, MoviesRagProperties properties) {
		List<Movie> movies = movieReader.read(properties.moviesFile());
		Map<Integer, float[]> vectors = embeddingsFile.load(properties.embeddingsFile());

		entries = movies.stream()
				.filter(movie -> vectors.containsKey(movie.id()))
				.map(movie -> new Entry(movie, vectors.get(movie.id())))
				.toList();

		Set<Integer> movieIds = movies.stream().map(Movie::id).collect(Collectors.toSet());
		long orphans = vectors.keySet().stream().filter(id -> !movieIds.contains(id)).count();
		if (orphans > 0) {
			log.warn("{} vectors belong to movies that are no longer in {}", orphans, properties.moviesFile());
		}
		log.info("Vector store ready: {} of {} movies have a vector", entries.size(), movies.size());
	}

	public List<Entry> entries() {
		return entries;
	}

	public int size() {
		return entries.size();
	}
}
