# Movies RAG

Work in progress. A Java RAG app that recommends movies from a free-text description of what you feel like watching, e.g. "a sad movie to watch on a rainy day with my girlfriend".

It uses a movie dataset from Kaggle: movies are embedded and kept in an in-memory vector store, the closest matches to your request are retrieved, and an LLM picks and explains the recommendations.

I'm building it to learn how RAG works, so it's going step by step.

## Dataset

The data comes from the [TMDB 5000 Movie Dataset](https://www.kaggle.com/datasets/tmdb/tmdb-movie-metadata) on Kaggle (4,803 movies). It isn't included in the repo.

1. Download the dataset and put `tmdb_5000_movies.csv` in `data/` (`tmdb_5000_credits.csv` isn't used).
2. Generate the cleaned file:

   ```
   pip install -r scripts/requirements.txt
   python scripts/prepare_dataset.py
   ```

This writes `data/movies.csv`, the file the app ingests.

## Running locally

You need JDK 25 and a Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey); the free tier is enough. Maven comes with the wrapper.

1. Copy `.env.example` to `.env` and set `GEMINI_API_KEY`. An environment variable with the same name works too and takes precedence.
2. From the repository root (the `.env` path is relative to it):

   ```
   ./mvnw spring-boot:run
   ```

   On Windows, `mvnw.cmd spring-boot:run`.
3. Check that it's up:

   ```
   curl http://localhost:8080/actuator/health
   ```

   It should answer `{"status":"UP"}`.

`./mvnw test` runs the tests. One of them starts the whole application, so it also needs the key.

## License

[MIT](LICENSE)
