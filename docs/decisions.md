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
