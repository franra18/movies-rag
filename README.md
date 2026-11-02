# Movies RAG

> 🚧 **Work in progress** — this project is being built step by step as a learning exercise.

A Retrieval-Augmented Generation (RAG) application in Java that recommends movies based on the *vibe* you describe in natural language.

```text
"a sad movie to watch on a rainy day with my girlfriend"
```

The app embeds a movie dataset (from Kaggle), retrieves the films whose descriptions best match your request, and uses an LLM to pick and explain the recommendations — only from movies that actually exist in the dataset.

## Status

- [x] Repository setup
- [ ] Dataset exploration and cleaning
- [ ] Architecture decisions
- [ ] Java application skeleton
- [ ] Ingestion, embeddings and vector store
- [ ] Retrieval
- [ ] LLM generation
- [ ] REST API and UI
- [ ] Tests, evaluation, Docker and CI

## Project structure

```text
docs/   Design notes and technical decisions
data/   Movie dataset (not committed, see docs once available)
```

## Secrets

API keys are **never committed**. Local secrets go in a `.env` file, which is git-ignored; `.env.example` lists the expected keys.

## License

[MIT](LICENSE)
