package io.github.franra18.moviesrag.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SearchFiltersTest {

	private static Movie movie(Integer year, Integer runtime) {
		return new Movie(1, "Title", year, "Overview.", "", List.of(), List.of(), runtime, 7.0, 10, "en");
	}

	@Test
	void noneLetsEverythingThrough() {
		assertTrue(SearchFilters.NONE.matches(movie(null, null)));
	}

	@Test
	void yearRangeIncludesBothEnds() {
		SearchFilters nineties = new SearchFilters(1990, 1999, null);

		assertTrue(nineties.matches(movie(1990, 100)));
		assertTrue(nineties.matches(movie(1999, 100)));
		assertFalse(nineties.matches(movie(1989, 100)));
		assertFalse(nineties.matches(movie(2000, 100)));
	}

	@Test
	void eachYearBoundWorksAlone() {
		assertTrue(new SearchFilters(2001, null, null).matches(movie(2015, 100)));
		assertFalse(new SearchFilters(2001, null, null).matches(movie(2000, 100)));
		assertTrue(new SearchFilters(null, 1979, null).matches(movie(1979, 100)));
		assertFalse(new SearchFilters(null, 1979, null).matches(movie(1980, 100)));
	}

	@Test
	void maxRuntimeIsInclusive() {
		SearchFilters short90 = new SearchFilters(null, null, 90);

		assertTrue(short90.matches(movie(2000, 90)));
		assertFalse(short90.matches(movie(2000, 91)));
	}

	@Test
	void unknownValuesFailTheirFilter() {
		assertFalse(new SearchFilters(1990, null, null).matches(movie(null, 100)));
		assertFalse(new SearchFilters(null, 1999, null).matches(movie(null, 100)));
		assertFalse(new SearchFilters(null, null, 90).matches(movie(2000, null)));
	}

	@Test
	void rejectsAnEmptyYearRange() {
		assertThrows(IllegalArgumentException.class, () -> new SearchFilters(2000, 1990, null));
	}
}
