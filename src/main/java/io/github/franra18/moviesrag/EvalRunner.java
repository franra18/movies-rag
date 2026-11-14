package io.github.franra18.moviesrag;

import io.github.franra18.moviesrag.model.ScoredMovie;
import io.github.franra18.moviesrag.model.SearchFilters;
import io.github.franra18.moviesrag.repository.MovieVectorStore;
import io.github.franra18.moviesrag.service.SearchService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the queries in eval/queries.json and writes a Markdown report to eval/results/, to compare
 * one version of the search with another. Started with the "eval" profile; --label=name is added
 * to the file name.
 */
@Component
@Profile("eval")
class EvalRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(EvalRunner.class);
	private static final Path QUERIES = Path.of("eval/queries.json");
	private static final Path RESULTS = Path.of("eval/results");

	record EvalQuery(String query, List<String> expected, SearchFilters filters) {
	}

	private final SearchService searchService;
	private final MovieVectorStore vectorStore;
	private final JsonMapper jsonMapper;
	private final int topK;

	EvalRunner(SearchService searchService, MovieVectorStore vectorStore, JsonMapper jsonMapper,
			MoviesRagProperties properties) {
		this.searchService = searchService;
		this.vectorStore = vectorStore;
		this.jsonMapper = jsonMapper;
		this.topK = properties.search().topK();
	}

	@Override
	public void run(ApplicationArguments args) throws IOException {
		List<EvalQuery> queries = jsonMapper.readValue(Files.readString(QUERIES, StandardCharsets.UTF_8),
				new TypeReference<List<EvalQuery>>() {
				});
		// An expected movie without a vector can't be found yet, so it isn't counted as a miss
		Set<String> withVector = vectorStore.entries().stream()
				.map(entry -> entry.movie().title())
				.collect(Collectors.toSet());

		StringBuilder sections = new StringBuilder();
		int queriesWithAHit = 0;
		int found = 0;
		int findable = 0;
		for (EvalQuery query : queries) {
			SearchFilters filters = query.filters() == null ? SearchFilters.NONE : query.filters();
			List<ScoredMovie> results = searchService.search(query.query(), filters);
			Set<String> resultTitles = results.stream().map(result -> result.movie().title()).collect(Collectors.toSet());
			List<String> canBeFound = query.expected().stream().filter(withVector::contains).toList();
			List<String> noVector = query.expected().stream().filter(title -> !withVector.contains(title)).toList();
			int hits = (int) canBeFound.stream().filter(resultTitles::contains).count();

			if (hits > 0) {
				queriesWithAHit++;
			}
			found += hits;
			findable += canBeFound.size();
			sections.append(section(query, filters, results, hits, canBeFound.size(), noVector));
		}

		String label = args.containsOption("label") ? args.getOptionValues("label").getFirst() : "";
		LocalDateTime now = LocalDateTime.now();
		String report = "# Search eval, " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
				+ (label.isEmpty() ? "" : " (" + label + ")") + "\n\n"
				+ "- " + vectorStore.size() + " movies with a vector, top-k " + topK + "\n"
				+ "- Queries with at least one expected movie in the results: " + queriesWithAHit + " of " + queries.size() + "\n"
				+ "- Expected movies found: " + found + " of " + findable + " that have a vector\n"
				+ sections;

		Files.createDirectories(RESULTS);
		String fileName = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"))
				+ (label.isEmpty() ? "" : "-" + label) + ".md";
		Path file = RESULTS.resolve(fileName);
		Files.writeString(file, report, StandardCharsets.UTF_8);
		log.info("{} of {} queries found an expected movie, report in {}", queriesWithAHit, queries.size(), file);
	}

	private static String section(EvalQuery query, SearchFilters filters, List<ScoredMovie> results, int hits,
			int canBeFound, List<String> noVector) {
		StringBuilder out = new StringBuilder("\n## ").append(query.query()).append("\n\n");
		List<String> applied = new ArrayList<>();
		if (filters.fromYear() != null) {
			applied.add("from " + filters.fromYear());
		}
		if (filters.toYear() != null) {
			applied.add("to " + filters.toYear());
		}
		if (filters.maxRuntime() != null) {
			applied.add("up to " + filters.maxRuntime() + " min");
		}
		if (!applied.isEmpty()) {
			out.append("Filters: ").append(String.join(", ", applied)).append("\n\n");
		}
		out.append("Expected found: ").append(hits).append(" of ").append(canBeFound);
		if (!noVector.isEmpty()) {
			out.append(" (no vector yet: ").append(String.join(", ", noVector)).append(")");
		}
		out.append("\n\n| # | Movie | Year | Score |\n|---|---|---|---|\n");
		for (int i = 0; i < results.size(); i++) {
			ScoredMovie result = results.get(i);
			boolean expected = query.expected().contains(result.movie().title());
			out.append("| ").append(i + 1)
					.append(" | ").append(result.movie().title()).append(expected ? " ✓" : "")
					.append(" | ").append(result.movie().year() == null ? "" : result.movie().year())
					.append(" | ").append(String.format(Locale.ROOT, "%.3f", result.score()))
					.append(" |\n");
		}
		return out.toString();
	}
}
