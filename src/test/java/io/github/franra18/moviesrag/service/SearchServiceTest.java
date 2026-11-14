package io.github.franra18.moviesrag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.franra18.moviesrag.MoviesRagProperties;
import io.github.franra18.moviesrag.model.ScoredMovie;
import io.github.franra18.moviesrag.model.SearchFilters;
import io.github.franra18.moviesrag.repository.EmbeddingsFile;
import io.github.franra18.moviesrag.repository.MovieCsvReader;
import io.github.franra18.moviesrag.repository.MovieVectorStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.embedding.EmbeddingModel;

class SearchServiceTest {

	@TempDir
	Path dir;

	private final EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
	private MovieVectorStore store;

	// Unit vectors, like the model's. Against the query {1, 0, 0}, One scores 1, Two 0.8 and Three 0
	@BeforeEach
	void createStore() throws IOException {
		Path movies = dir.resolve("movies.csv");
		Files.writeString(movies, """
				id,title,year,overview,tagline,genres,keywords,runtime,vote_average,vote_count,original_language
				1,One,1995,First overview.,,Drama,,120,7.0,10,en
				2,Two,2005,Second overview.,,Drama,,85,7.0,10,en
				3,Three,1998,Third overview.,,Drama,,95,7.0,10,en
				""");
		Path embeddings = dir.resolve("embeddings.csv");
		EmbeddingsFile embeddingsFile = new EmbeddingsFile(3);
		embeddingsFile.append(embeddings, 1, new float[] {1, 0, 0});
		embeddingsFile.append(embeddings, 2, new float[] {0.8f, 0.6f, 0});
		embeddingsFile.append(embeddings, 3, new float[] {0, 1, 0});
		store = new MovieVectorStore(new MovieCsvReader(), embeddingsFile, properties(movies, embeddings, 5));
		when(embeddingModel.embed(anyString())).thenReturn(new float[] {1, 0, 0});
	}

	private static MoviesRagProperties properties(Path movies, Path embeddings, int topK) {
		return new MoviesRagProperties(movies, embeddings, new MoviesRagProperties.Ingest(0, 0, Duration.ZERO),
				new MoviesRagProperties.Search(topK));
	}

	private SearchService service(int topK) {
		return new SearchService(embeddingModel, store, properties(null, null, topK));
	}

	private static List<String> titles(List<ScoredMovie> results) {
		return results.stream().map(result -> result.movie().title()).toList();
	}

	@Test
	void ranksByCosineSimilarity() {
		List<ScoredMovie> results = service(5).search("sad movie", SearchFilters.NONE);

		assertEquals(List.of("One", "Two", "Three"), titles(results));
		assertEquals(1.0, results.get(0).score(), 1e-6);
		assertEquals(0.8, results.get(1).score(), 1e-6);
		assertEquals(0.0, results.get(2).score(), 1e-6);
	}

	@Test
	void returnsAtMostTopK() {
		assertEquals(List.of("One", "Two"), titles(service(2).search("sad movie", SearchFilters.NONE)));
	}

	@Test
	void filtersBeforeRanking() {
		assertEquals(List.of("One", "Three"), titles(service(5).search("sad movie", new SearchFilters(1990, 1999, null))));
		assertEquals(List.of("Two"), titles(service(5).search("sad movie", new SearchFilters(null, null, 90))));
	}

	@Test
	void embedsTheRequestWithTheQueryPrefix() {
		service(5).search("sad movie", SearchFilters.NONE);

		verify(embeddingModel).embed("task: search result | query: sad movie");
	}

	@Test
	void rejectsAnEmptyRequest() {
		assertThrows(IllegalArgumentException.class, () -> service(5).search("  ", SearchFilters.NONE));
	}

	@Test
	void dotOfUnitVectorsIsTheirCosine() {
		assertEquals(0.96, SearchService.dot(new float[] {0.6f, 0.8f, 0}, new float[] {0.8f, 0.6f, 0}), 1e-6);
	}
}
