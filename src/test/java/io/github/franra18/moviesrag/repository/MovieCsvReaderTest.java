package io.github.franra18.moviesrag.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.franra18.moviesrag.model.Movie;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MovieCsvReaderTest {

	private static final String HEADER = "id,title,year,overview,tagline,genres,keywords,runtime,vote_average,vote_count,original_language\n";

	@TempDir
	Path dir;

	private final MovieCsvReader reader = new MovieCsvReader();

	private Path write(String content) throws IOException {
		return Files.writeString(dir.resolve("movies.csv"), content);
	}

	private List<Movie> read(String rows) throws IOException {
		return reader.read(write(HEADER + rows));
	}

	@Test
	void readsEveryColumn() throws IOException {
		Movie movie = read(
				"5,Four Rooms,1995,A bellhop's first night.,Twelve guests.,Crime|Comedy,hotel|bet,98,6.5,530,en\n")
				.getFirst();

		assertEquals(5, movie.id());
		assertEquals("Four Rooms", movie.title());
		assertEquals(1995, movie.year());
		assertEquals(List.of("Crime", "Comedy"), movie.genres());
		assertEquals(List.of("hotel", "bet"), movie.keywords());
		assertEquals(98, movie.runtime());
		assertEquals(6.5, movie.voteAverage());
		assertEquals(530, movie.voteCount());
		assertEquals("en", movie.originalLanguage());
	}

	@Test
	void quotedFieldKeepsCommasQuotesAndLineBreaks() throws IOException {
		Movie movie = read("""
				1,Warlock,1989,"A witch, a hunter
				and a ""warlock"".",,Horror,,103,6.0,100,en
				""").getFirst();

		assertEquals("A witch, a hunter\nand a \"warlock\".", movie.overview());
	}

	@Test
	void emptyValuesBecomeNullOrEmpty() throws IOException {
		Movie movie = read("7,No Data,,Some overview.,,,,,5.0,3,fr\n").getFirst();

		assertNull(movie.year());
		assertNull(movie.runtime());
		assertEquals("", movie.tagline());
		assertEquals(List.of(), movie.genres());
		assertEquals(List.of(), movie.keywords());
	}

	@Test
	void malformedRowsAreSkipped() throws IOException {
		List<Movie> movies = read("""
				abc,Bad Id,2000,Overview.,,Drama,,90,6.0,10,en
				8,Too Short,2000
				9,Good,2001,Overview.,,Drama,,90,7.0,20,en
				""");

		assertEquals(1, movies.size());
		assertEquals(9, movies.getFirst().id());
	}

	@Test
	void missingColumnFailsBeforeReadingRows() throws IOException {
		Path file = write("id,title\n1,X\n");

		assertThrows(IllegalStateException.class, () -> reader.read(file));
	}
}
