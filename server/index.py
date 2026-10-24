"""Vercel FastAPI entrypoint.

Vercel's zero-config FastAPI detection only recognises an ASGI ``app`` exported
from ``app.py``/``index.py``/``server.py``/``main.py``/``wsgi.py``/``asgi.py`` at
the project root (or the same names inside ``src/`` or ``app/``).  This module is
that entrypoint: it re-exports the real application assembled in ``api/index.py``.
"""

from api.index import app  # noqa: F401  (Vercel looks for a module-level `app`)

__all__ = ["app"]
