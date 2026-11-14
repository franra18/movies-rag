package io.github.franra18.moviesrag.model;

/**
 * Conditions a movie must meet to appear in a search. A null field doesn't filter anything, and
 * every bound is inclusive: the 90s are fromYear 1990, toYear 1999.
 *
 * @param maxRuntime minutes
 */
public record SearchFilters(Integer fromYear, Integer toYear, Integer maxRuntime) {

	public static final SearchFilters NONE = new SearchFilters(null, null, null);

	public SearchFilters {
		if (fromYear != null && toYear != null && fromYear > toYear) {
			throw new IllegalArgumentException("fromYear " + fromYear + " is after toYear " + toYear);
		}
	}

	/** A movie whose year or runtime is unknown fails the filters on that field. */
	public boolean matches(Movie movie) {
		if ((fromYear != null || toYear != null) && movie.year() == null) {
			return false;
		}
		if (fromYear != null && movie.year() < fromYear) {
			return false;
		}
		if (toYear != null && movie.year() > toYear) {
			return false;
		}
		if (maxRuntime != null && (movie.runtime() == null || movie.runtime() > maxRuntime)) {
			return false;
		}
		return true;
	}
}
