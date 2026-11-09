package io.github.franra18.moviesrag.service;

import io.github.franra18.moviesrag.MoviesRagProperties;
import io.github.franra18.moviesrag.model.Movie;
import io.github.franra18.moviesrag.repository.EmbeddingsFile;
import io.github.franra18.moviesrag.repository.MovieCsvReader;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class IngestionService {

	private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
	private static final int PROGRESS_EVERY = 25;

	private final EmbeddingModel embeddingModel;
	private final MovieCsvReader movieReader;
	private final EmbeddingsFile embeddingsFile;
	private final MoviesRagProperties properties;

	public IngestionService(EmbeddingModel embeddingModel, MovieCsvReader movieReader,
			EmbeddingsFile embeddingsFile, MoviesRagProperties properties) {
		this.embeddingModel = embeddingModel;
		this.movieReader = movieReader;
		this.embeddingsFile = embeddingsFile;
		this.properties = properties;
	}

	/** Embeds the movies missing from embeddings.csv, up to the configured limit. */
	public void ingest() {
		List<Movie> movies = movieReader.read(properties.moviesFile());
		Set<Integer> done = embeddingsFile.load(properties.embeddingsFile()).keySet();
		List<Movie> pending = movies.stream().filter(movie -> !done.contains(movie.id())).toList();
		int limit = properties.ingest().limit();
		if (limit > 0 && pending.size() > limit) {
			pending = pending.subList(0, limit);
		}
		log.info("{} of {} movies already embedded, {} in this run", done.size(), movies.size(), pending.size());

		Instant start = Instant.now();
		int embedded = 0;
		for (Movie movie : pending) {
			Optional<float[]> vector = embed(movie);
			if (vector.isEmpty()) {
				log.warn("Still getting 429 after {} waits, so the daily quota is probably used up. Run again tomorrow",
						properties.ingest().rateLimitRetries());
				break;
			}
			embeddingsFile.append(properties.embeddingsFile(), movie.id(), vector.get());
			embedded++;
			if (embedded % PROGRESS_EVERY == 0) {
				log.info("{}/{} embedded ({}s)", embedded, pending.size(), Duration.between(start, Instant.now()).toSeconds());
			}
		}
		log.info("Run finished: {} embedded, {} of {} in total", embedded, done.size() + embedded, movies.size());
	}

	/** Empty when the API still answers 429 after every retry. */
	private Optional<float[]> embed(Movie movie) {
		String text = EmbeddingText.forMovie(movie);
		MoviesRagProperties.Ingest config = properties.ingest();
		for (int attempt = 0; ; attempt++) {
			try {
				return Optional.of(embeddingModel.embed(text));
			} catch (RuntimeException e) {
				if (!isRateLimit(e)) {
					throw new IllegalStateException("Embedding failed for movie " + movie.id(), e);
				}
				if (attempt == config.rateLimitRetries()) {
					return Optional.empty();
				}
				log.info("429 on movie {}, waiting {}s", movie.id(), config.rateLimitWait().toSeconds());
				sleep(config.rateLimitWait());
			}
		}
	}

	// Matched on the message: the exception class depends on how Spring AI wraps the Gemini client
	private static boolean isRateLimit(Throwable e) {
		for (Throwable t = e; t != null; t = t.getCause()) {
			String message = t.getMessage();
			if (message != null && (message.contains("429") || message.contains("RESOURCE_EXHAUSTED"))) {
				return true;
			}
		}
		return false;
	}

	private static void sleep(Duration duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for the rate limit", e);
		}
	}
}
