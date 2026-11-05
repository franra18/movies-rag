package io.github.franra18.moviesrag.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MovieTest {

	private static Movie movie(List<String> genres, Integer runtime) {
		return new Movie(1, "Title", 2000, "Overview", "", genres, null, runtime, 7.0, 10, "en");
	}

	@Test
	void zeroRuntimeIsMissing() {
		assertNull(movie(List.of(), 0).runtime());
		assertEquals(98, movie(List.of(), 98).runtime());
	}

	@Test
	void missingListsBecomeEmpty() {
		Movie movie = movie(null, 98);
		assertEquals(List.of(), movie.genres());
		assertEquals(List.of(), movie.keywords());
	}

	@Test
	void missingTaglineBecomesEmpty() {
		Movie movie = new Movie(1, "Title", 2000, "Overview", null, List.of(), List.of(), 98, 7.0, 10, "en");
		assertEquals("", movie.tagline());
	}

	@Test
	void listsAreImmutableCopies() {
		List<String> genres = new ArrayList<>(List.of("Crime"));
		Movie movie = movie(genres, 98);
		genres.add("Comedy");
		assertEquals(List.of("Crime"), movie.genres());
		assertThrows(UnsupportedOperationException.class, () -> movie.genres().add("Drama"));
	}
}
