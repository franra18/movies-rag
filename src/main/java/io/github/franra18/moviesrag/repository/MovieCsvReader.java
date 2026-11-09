package io.github.franra18.moviesrag.repository;

import io.github.franra18.moviesrag.model.Movie;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Reads data/movies.csv, the file written by scripts/prepare_dataset.py. */
@Component
public class MovieCsvReader {

	private static final Logger log = LoggerFactory.getLogger(MovieCsvReader.class);

	private static final List<String> COLUMNS = List.of("id", "title", "year", "overview", "tagline",
			"genres", "keywords", "runtime", "vote_average", "vote_count", "original_language");

	private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
			.setHeader()
			.setSkipHeaderRecord(true)
			.get();

	public List<Movie> read(Path path) {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return read(reader);
		} catch (IOException e) {
			throw new UncheckedIOException("Can't read " + path, e);
		}
	}

	List<Movie> read(Reader reader) throws IOException {
		List<Movie> movies = new ArrayList<>();
		try (CSVParser parser = FORMAT.parse(reader)) {
			if (!parser.getHeaderNames().containsAll(COLUMNS)) {
				throw new IllegalStateException("Expected columns " + COLUMNS + ", found " + parser.getHeaderNames());
			}
			for (CSVRecord row : parser) {
				try {
					movies.add(toMovie(row));
				} catch (IllegalArgumentException e) {
					// row.get() also throws this when the row has fewer columns than the header
					log.warn("Skipping record {}: {}", row.getRecordNumber(), e.getMessage());
				}
			}
		}
		return movies;
	}

	private static Movie toMovie(CSVRecord row) {
		return new Movie(
				Integer.parseInt(row.get("id")),
				row.get("title"),
				nullableInt(row.get("year")),
				row.get("overview"),
				row.get("tagline"),
				list(row.get("genres")),
				list(row.get("keywords")),
				nullableInt(row.get("runtime")),
				Double.parseDouble(row.get("vote_average")),
				Integer.parseInt(row.get("vote_count")),
				row.get("original_language"));
	}

	private static Integer nullableInt(String value) {
		return value.isBlank() ? null : Integer.valueOf(value);
	}

	private static List<String> list(String value) {
		return value.isBlank() ? List.of() : List.of(value.split("\\|"));
	}
}
