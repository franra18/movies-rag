"""Embed one request and three movies, then compare them with cosine similarity.

Usage: python scripts/embedding_experiment.py
Needs GEMINI_API_KEY in .env. Makes 8 embedding requests (with and without prefixes).
"""
import csv
import math
from pathlib import Path

from dotenv import load_dotenv
from google import genai
from google.genai import types

ROOT = Path(__file__).resolve().parent.parent
MODEL = "gemini-embedding-2"
DIMENSIONS = 768

QUERY = "a sad movie to watch on a rainy day with my girlfriend"
MOVIES = ["The Notebook", "Blue Valentine", "Crank"]


def cosine(a: list[float], b: list[float]) -> float:
    dot = sum(x * y for x, y in zip(a, b))
    norm_a = math.sqrt(sum(x * x for x in a))
    norm_b = math.sqrt(sum(y * y for y in b))
    return dot / (norm_a * norm_b)


def movie_text(movie: dict) -> str:
    genres = movie["genres"].replace("|", ", ")
    keywords = movie["keywords"].replace("|", ", ")
    return f"{movie['tagline']} Genres: {genres}. Keywords: {keywords}. {movie['overview']}"


def embed(client: genai.Client, text: str) -> list[float]:
    # One text per request: gemini-embedding-2 merges several texts into a single vector.
    result = client.models.embed_content(
        model=MODEL,
        contents=text,
        config=types.EmbedContentConfig(output_dimensionality=DIMENSIONS),
    )
    return result.embeddings[0].values


def main() -> None:
    load_dotenv(ROOT / ".env")
    client = genai.Client()

    with open(ROOT / "data" / "movies.csv", encoding="utf-8") as f:
        movies = {row["title"]: row for row in csv.DictReader(f)}

    variants = {
        "with prefixes": (
            f"task: search result | query: {QUERY}",
            lambda m: f"title: {m['title']} | text: {movie_text(m)}",
        ),
        "without prefixes": (
            QUERY,
            lambda m: f"{m['title']}. {movie_text(m)}",
        ),
    }

    for name, (query_text, document_text) in variants.items():
        query_vector = embed(client, query_text)
        print(f"\n{name}  ({len(query_vector)} dims)")
        for title in MOVIES:
            score = cosine(query_vector, embed(client, document_text(movies[title])))
            print(f"  {score:.4f}  {title}")


if __name__ == "__main__":
    main()
