package io.github.franra18.moviesrag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.franra18.moviesrag.MoviesRagProperties;
import io.github.franra18.moviesrag.repository.EmbeddingsFile;
import io.github.franra18.moviesrag.repository.MovieCsvReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.embedding.EmbeddingModel;

class IngestionServiceTest {

	private static final float[] VECTOR = {0.1f, 0.2f, 0.3f};

	@TempDir
	Path dir;

	private final EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
	private final EmbeddingsFile embeddingsFile = new EmbeddingsFile(3);

	private IngestionService service(int limit) throws IOException {
		Path movies = dir.resolve("movies.csv");
		Files.writeString(movies, """
				id,title,year,overview,tagline,genres,keywords,runtime,vote_average,vote_count,original_language
				1,One,2001,First overview.,,Drama,,90,7.0,10,en
				2,Two,2002,Second overview.,,Drama,,90,7.0,10,en
				3,Three,2003,Third overview.,,Drama,,90,7.0,10,en
				""");
		MoviesRagProperties properties = new MoviesRagProperties(movies, embeddings(),
				new MoviesRagProperties.Ingest(limit, 2, Duration.ZERO));
		return new IngestionService(embeddingModel, new MovieCsvReader(), embeddingsFile, properties);
	}

	private Path embeddings() {
		return dir.resolve("embeddings.csv");
	}

	private Set<Integer> embeddedIds() {
		return embeddingsFile.load(embeddings()).keySet();
	}

	@Test
	void embedsEveryMovie() throws IOException {
		when(embeddingModel.embed(anyString())).thenReturn(VECTOR);

		service(0).ingest();

		assertEquals(Set.of(1, 2, 3), embeddedIds());
	}

	@Test
	void skipsMoviesAlreadyInTheFile() throws IOException {
		embeddingsFile.append(embeddings(), 2, VECTOR);
		when(embeddingModel.embed(anyString())).thenReturn(VECTOR);

		service(0).ingest();

		assertEquals(Set.of(1, 2, 3), embeddedIds());
		verify(embeddingModel, times(2)).embed(anyString());
		verify(embeddingModel, never()).embed(startsWith("title: Two "));
	}

	@Test
	void limitCapsTheRun() throws IOException {
		when(embeddingModel.embed(anyString())).thenReturn(VECTOR);

		service(2).ingest();

		assertEquals(Set.of(1, 2), embeddedIds());
	}

	@Test
	void retriesAfterA429() throws IOException {
		when(embeddingModel.embed(anyString()))
				.thenThrow(new RuntimeException("429 Too Many Requests"))
				.thenReturn(VECTOR);

		service(0).ingest();

		assertEquals(Set.of(1, 2, 3), embeddedIds());
		verify(embeddingModel, times(4)).embed(anyString());
	}

	@Test
	void stopsQuietlyWhenThe429Persists() throws IOException {
		when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("RESOURCE_EXHAUSTED"));

		service(0).ingest();

		assertEquals(Set.of(), embeddedIds());
		verify(embeddingModel, times(3)).embed(anyString());
	}

	@Test
	void otherErrorsStopTheRunAndKeepWhatWasSaved() throws IOException {
		when(embeddingModel.embed(anyString())).thenReturn(VECTOR);
		when(embeddingModel.embed(startsWith("title: Two "))).thenThrow(new RuntimeException("500 Internal error"));

		IngestionService service = service(0);
		assertThrows(IllegalStateException.class, service::ingest);

		assertEquals(Set.of(1), embeddedIds());
	}
}
