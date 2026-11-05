# Decisions

## License (2026-11-02)

MIT. Considered Apache 2.0 too, but MIT is simpler and enough for a personal project.

## Dataset (2026-11-02)

TMDB 5000 Movie Dataset from Kaggle, built from the TMDb API (license "Other", TMDb terms of use). The Movies Dataset (45k movies, 239 MB) has more titles but is much heavier and messier; 4,803 movies is enough to start and keeps embedding costs low.

No CSV is committed, neither raw nor cleaned: the data belongs to TMDb and `scripts/prepare_dataset.py` regenerates it in seconds.

## Data cleaning (2026-11-02)

Done once in Python (pandas) rather than in the Java ingestion. The raw CSV stores genres and keywords as JSON inside cells; flattening that in pandas takes a few lines, and the Java side only has to read a flat CSV.

- Text to embed: title, overview, tagline, genres, keywords.
- Metadata for filtering: year, runtime, vote_average, vote_count, original_language.
- Dropped: unreleased movies and overviews under 10 words (24 movies). Those overviews say nothing about the mood of the movie.
- No minimum vote count.
- `runtime = 0` is treated as missing.

## Framework (2026-11-03)

Spring Boot with Spring AI. I already work with Spring Boot, and Spring AI gives me the chat and embedding clients plus config through `application.yml`. LangChain4j and plain HTTP calls were the other options.

## Models (2026-11-03)

Both from the Gemini API, so there's a single API key.

- Embeddings: `gemini-embedding-2` at 768 dimensions. A quarter of the size of the default 3072, and this model normalizes truncated vectors itself. It replaces `task_type` with prefixes in the text, which I build myself:
  - movie: `title: {title} | text: {tagline, genres, keywords, overview}`
  - request: `task: search result | query: {request}`
- LLM: `gemini-2.5-flash-lite`. It only has to pick and explain a few movies from a short list, so the cheapest model should do. Changing it is one line of config.

Spring AI's Google GenAI embedding client sends a whole batch of texts in one `embedContent` call, and `gemini-embedding-2` answers that with a single aggregated vector instead of one per text. Movies are embedded one per request.

## Vector store (2026-11-03)

My own in-memory store: a list of movies with their vectors, cosine similarity against the request, top-k. With 4,779 vectors there's no need for a database yet.

Vectors are saved to `data/embeddings.csv` (`id` + vector) so they aren't requested again on every start. Separate from `movies.csv` because `prepare_dataset.py` rewrites that file, and because I can keep one embeddings file per text template to compare them. If the API rate limit cuts the ingestion halfway, it resumes from the ids already saved.

`SimpleVectorStore` was ruled out: it always computes the embeddings itself when adding documents, so it can't load precomputed ones, and it would batch them.

## One document per movie (2026-11-03)

No chunking. The longest text is under 200 words, far below the model's input limit.

## Gemini API free tier (2026-11-03)

Staying on the free tier. For `gemini-embedding-2` it allows 100 requests per minute, 30K tokens per minute and 1,000 requests per day. At one request per movie, the full ingestion takes about 5 days, so it has to wait between calls and resume from what's already in `embeddings.csv`. Requests made while testing count toward the same daily limit. While I tune the text template I'll work with a subset and only run the full ingestion at the end. Enabling billing would cost a few cents per full ingestion if waiting becomes a problem.

## Java and build (2026-11-05)

Java 25 with Maven. Spring AI 2.0 needs Spring Boot 4, which needs Java 17 or later, so any current LTS works; 25 is the newest one. Maven because most Spring AI examples use it.

## Package structure (2026-11-05)

By layer: `controller`, `service`, `repository`, `model`. It's the layout I already use at work. The vector store goes in `repository`, since it's where the movies and their vectors are kept.

## Model abstractions (2026-11-05)

Spring AI's `EmbeddingModel` and `ChatClient` are used directly, without interfaces of my own on top. Changing provider is already a config change. The prefixes and the one-movie-per-request rule live in the service that calls `EmbeddingModel`.

## API key (2026-11-05)

`application.yml` imports the `.env` file the Python scripts already use (`spring.config.import: optional:file:.env[.properties]`), so there's one place for the key. `optional:` keeps the app starting when the variable comes from the environment instead, as it will in CI or Docker.
