package io.github.franra18.moviesrag.service;

import io.github.franra18.moviesrag.MoviesRagProperties;
import io.github.franra18.moviesrag.model.ScoredMovie;
import io.github.franra18.moviesrag.model.SearchFilters;
import io.github.franra18.moviesrag.repository.MovieVectorStore;
import java.util.Comparator;
import java.util.List;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!ingest")
public class SearchService {

	private final EmbeddingModel embeddingModel;
	private final MovieVectorStore vectorStore;
	private final int topK;

	public SearchService(EmbeddingModel embeddingModel, MovieVectorStore vectorStore, MoviesRagProperties properties) {
		this.embeddingModel = embeddingModel;
		this.vectorStore = vectorStore;
		this.topK = properties.search().topK();
	}

	/** The movies closest to the request, best first. At most top-k of them. */
	public List<ScoredMovie> search(String request, SearchFilters filters) {
		if (request == null || request.isBlank()) {
			throw new IllegalArgumentException("Empty request");
		}
		float[] query = embeddingModel.embed(EmbeddingText.forQuery(request));
		return vectorStore.entries().stream()
				.filter(entry -> filters.matches(entry.movie()))
				.map(entry -> new ScoredMovie(entry.movie(), dot(query, entry.vector())))
				.sorted(Comparator.comparingDouble(ScoredMovie::score).reversed())
				.limit(topK)
				.toList();
	}

	// gemini-embedding-2 returns unit vectors (see docs/decisions.md), and for those the dot
	// product is already the cosine similarity
	static double dot(float[] a, float[] b) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i] * b[i];
		}
		return sum;
	}
}
