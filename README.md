# Movies RAG

Work in progress. A Java RAG app that recommends movies from a free-text description of what you feel like watching, e.g. "a sad movie to watch on a rainy day with my girlfriend".

It uses a movie dataset from Kaggle: movies are embedded and stored in a vector database, the closest matches to your request are retrieved, and an LLM picks and explains the recommendations.

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

## Roadmap

- [x] Repository setup
- [ ] Dataset exploration and cleaning
- [ ] Architecture decisions
- [ ] Java application skeleton
- [ ] Ingestion, embeddings and vector store
- [ ] Retrieval
- [ ] LLM generation
- [ ] REST API and UI
- [ ] Tests, evaluation, Docker and CI

## License

[MIT](LICENSE)
