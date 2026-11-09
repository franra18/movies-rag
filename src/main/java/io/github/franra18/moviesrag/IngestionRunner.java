package io.github.franra18.moviesrag;

import io.github.franra18.moviesrag.service.IngestionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Runs the ingestion when the app starts with the "ingest" profile. */
@Component
@Profile("ingest")
class IngestionRunner implements ApplicationRunner {

	private final IngestionService ingestionService;

	IngestionRunner(IngestionService ingestionService) {
		this.ingestionService = ingestionService;
	}

	@Override
	public void run(ApplicationArguments args) {
		ingestionService.ingest();
	}
}
