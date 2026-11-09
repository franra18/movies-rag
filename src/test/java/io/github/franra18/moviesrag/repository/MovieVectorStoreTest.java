package io.github.franra18.moviesrag.repository;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.franra18.moviesrag.MoviesRagProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MovieVectorStoreTest {

	@TempDir
	Path dir;

	@Test
	void keepsOnlyMoviesThatHaveAVector() throws IOException {
		Path movies = dir.resolve("movies.csv");
		Files.writeString(movies, """
				id,title,year,overview,tagline,genres,keywords,runtime,vote_average,vote_count,original_language
				1,One,2001,First overview.,,Drama,,90,7.0,10,en
				2,Two,2002,Second overview.,,Drama,,90,7.0,10,en
				""");
		Path embeddings = dir.resolve("embeddings.csv");
		EmbeddingsFile embeddingsFile = new EmbeddingsFile(3);
		embeddingsFile.append(embeddings, 2, new float[] {0.1f, 0.2f, 0.3f});
		embeddingsFile.append(embeddings, 99, new float[] {0.4f, 0.5f, 0.6f});

		MovieVectorStore store = new MovieVectorStore(new MovieCsvReader(), embeddingsFile,
				new MoviesRagProperties(movies, embeddings, new MoviesRagProperties.Ingest(0, 0, Duration.ZERO)));

		assertEquals(1, store.size());
		MovieVectorStore.Entry entry = store.entries().getFirst();
		assertEquals("Two", entry.movie().title());
		assertArrayEquals(new float[] {0.1f, 0.2f, 0.3f}, entry.vector());
	}
}
