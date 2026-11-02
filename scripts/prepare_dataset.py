"""Build data/movies.csv from the raw TMDB 5000 dataset.

Usage: python scripts/prepare_dataset.py
Expects data/tmdb_5000_movies.csv (see README for the download link).
"""
import json
from pathlib import Path

import pandas as pd

DATA_DIR = Path(__file__).resolve().parent.parent / "data"
RAW_FILE = DATA_DIR / "tmdb_5000_movies.csv"
OUT_FILE = DATA_DIR / "movies.csv"

# Shorter overviews ("The 8th installment in the Khiladi series.") say nothing
# about the movie's mood, so they only add noise to retrieval.
MIN_OVERVIEW_WORDS = 10

COLUMNS = [
    "id", "title", "year", "overview", "tagline", "genres", "keywords",
    "runtime", "vote_average", "vote_count", "original_language",
]


def names(json_list: str) -> str:
    """'[{"id": 28, "name": "Action"}, ...]' -> 'Action|...'"""
    return "|".join(item["name"] for item in json.loads(json_list))


def main() -> None:
    df = pd.read_csv(RAW_FILE)
    total = len(df)

    df["overview"] = df["overview"].fillna("").str.strip()
    df = df[df["status"] == "Released"]
    df = df[df["overview"].str.split().str.len() >= MIN_OVERVIEW_WORDS]

    df["genres"] = df["genres"].map(names)
    df["keywords"] = df["keywords"].map(names)
    df["year"] = pd.to_datetime(df["release_date"], errors="coerce").dt.year.astype("Int64")
    # TMDB uses 0 for unknown runtime
    df["runtime"] = df["runtime"].where(df["runtime"] > 0).astype("Int64")
    df["tagline"] = df["tagline"].fillna("").str.strip()

    df = df[COLUMNS].sort_values("id")
    df.to_csv(OUT_FILE, index=False)
    print(f"{len(df)} of {total} movies written to {OUT_FILE.relative_to(DATA_DIR.parent)}")


if __name__ == "__main__":
    main()
