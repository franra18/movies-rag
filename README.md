# Movies RAG

Work in progress. A Java RAG app that recommends movies from a free-text description of what you feel like watching, e.g. "a sad movie to watch on a rainy day with my girlfriend".

It uses a movie dataset from Kaggle: movies are embedded and stored in a vector database, the closest matches to your request are retrieved, and an LLM picks and explains the recommendations.

I'm building it to learn how RAG works, so it's going step by step.

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
