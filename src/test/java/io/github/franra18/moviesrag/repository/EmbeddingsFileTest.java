package io.github.franra18.moviesrag.repository;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmbeddingsFileTest {

	@TempDir
	Path dir;

	private final EmbeddingsFile file = new EmbeddingsFile(3);

	private Path path() {
		return dir.resolve("embeddings.csv");
	}

	@Test
	void appendWritesOneLinePerMovie() throws IOException {
		file.append(path(), 5, new float[] {0.1f, -0.2f, 0.3f});

		assertEquals("5,0.1 -0.2 0.3\n", Files.readString(path()));
	}

	@Test
	void loadReturnsWhatWasAppended() {
		float[] vector = {0.123456789f, -1.0E-5f, 1f};
		file.append(path(), 5, vector);
		file.append(path(), 11, new float[] {0f, 0f, 0f});

		Map<Integer, float[]> vectors = file.load(path());

		assertEquals(Set.of(5, 11), vectors.keySet());
		assertArrayEquals(vector, vectors.get(5));
	}

	@Test
	void missingFileLoadsEmpty() {
		assertTrue(file.load(path()).isEmpty());
	}

	@Test
	void invalidLinesAreSkipped() throws IOException {
		Files.writeString(path(), """
				1,0.1 0.2
				2,0.1 x 0.3
				abc,0.1 0.2 0.3
				no comma
				3,0.1 0.2 0.3
				""");

		assertEquals(Set.of(3), file.load(path()).keySet());
	}

	@Test
	void unfinishedLastLineIsDroppedSoTheNextAppendStartsClean() throws IOException {
		Files.writeString(path(), "1,0.1 0.2 0.3\n2,0.4 0.5");

		assertEquals(Set.of(1), file.load(path()).keySet());

		file.append(path(), 3, new float[] {0.7f, 0.8f, 0.9f});
		assertEquals(Set.of(1, 3), file.load(path()).keySet());
	}

	@Test
	void appendRejectsWrongSize() {
		assertThrows(IllegalArgumentException.class, () -> file.append(path(), 1, new float[] {0.1f}));
	}
}
