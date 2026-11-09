package io.github.franra18.moviesrag;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("movies-rag")
public record MoviesRagProperties(Path moviesFile, Path embeddingsFile, Ingest ingest) {

	/**
	 * @param limit             most movies embedded in one run, 0 for no limit
	 * @param rateLimitRetries  waits after a 429 before giving up on the run
	 */
	public record Ingest(int limit, int rateLimitRetries, Duration rateLimitWait) {
	}
}
