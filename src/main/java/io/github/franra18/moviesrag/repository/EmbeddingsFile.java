package io.github.franra18.moviesrag.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** data/embeddings.csv: one line per movie, "id,v1 v2 … vN". */
@Component
public class EmbeddingsFile {

	private static final Logger log = LoggerFactory.getLogger(EmbeddingsFile.class);

	private final int dimensions;

	public EmbeddingsFile(@Value("${spring.ai.google.genai.embedding.text.dimensions}") int dimensions) {
		this.dimensions = dimensions;
	}

	public Map<Integer, float[]> load(Path path) {
		Map<Integer, float[]> vectors = new HashMap<>();
		if (Files.notExists(path)) {
			return vectors;
		}
		try {
			String content = Files.readString(path, StandardCharsets.UTF_8);
			List<String> lines = new ArrayList<>(content.lines().toList());
			if (!content.isEmpty() && !content.endsWith("\n")) {
				// The last append was cut off. Drop it now, or the next append would be glued to it
				lines.removeLast();
				log.warn("Dropping unfinished last line of {}", path);
				Files.writeString(path, lines.isEmpty() ? "" : String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
			}
			for (int i = 0; i < lines.size(); i++) {
				try {
					parse(lines.get(i), vectors);
				} catch (IllegalArgumentException e) {
					log.warn("Skipping line {} of {}: {}", i + 1, path, e.getMessage());
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Can't read " + path, e);
		}
		return vectors;
	}

	public void append(Path path, int id, float[] vector) {
		if (vector.length != dimensions) {
			throw new IllegalArgumentException("Expected " + dimensions + " numbers, got " + vector.length);
		}
		StringBuilder line = new StringBuilder().append(id).append(',');
		for (int i = 0; i < vector.length; i++) {
			if (i > 0) {
				line.append(' ');
			}
			line.append(vector[i]);
		}
		line.append('\n');
		try {
			Files.writeString(path, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new UncheckedIOException("Can't write to " + path, e);
		}
	}

	private void parse(String line, Map<Integer, float[]> vectors) {
		int comma = line.indexOf(',');
		if (comma < 0) {
			throw new IllegalArgumentException("no comma");
		}
		int id = Integer.parseInt(line.substring(0, comma));
		String[] values = line.substring(comma + 1).split(" ");
		if (values.length != dimensions) {
			throw new IllegalArgumentException("expected " + dimensions + " numbers, found " + values.length);
		}
		float[] vector = new float[dimensions];
		for (int i = 0; i < dimensions; i++) {
			vector[i] = Float.parseFloat(values[i]);
		}
		vectors.put(id, vector);
	}
}
