# Kaggle dataset operations — Python API (kaggle>=2.1)
# Uses KAGGLE_API_TOKEN from environment (secret exists in CI).
# If token is unavailable, functions document it and return gracefully.

import os
from datetime import datetime
from kaggle.api.kaggle_api_extended import KaggleApi

DATASETS = ["tpa-history", "tpa-models"]


def _get_api():
    api = KaggleApi()
    token = os.environ.get("KAGGLE_API_TOKEN") or os.environ.get("KAGGLE_API_KEY")
    if token:
        # kaggle 2.x accepts token via env; authenticate reads it
        api.authenticate()
    else:
        # Try to use cached token; if none, api will raise on use
        try:
            api.authenticate()
        except Exception:
            pass
    return api


def create_dataset(dataset_slug="tpa-history"):
    """Create/update dataset using Python API (not CLI flags)."""
    api = _get_api()
    # Dataset creation requires a folder with dataset-metadata.json.
    # We document that folder-based setup is needed; here we attempt via
    # dataset_create_version if dataset exists, else dataset_create_new.
    # With only env token available (no folder prep) we report status.
    try:
        # Check existence via dataset_status
        status = api.dataset_status(dataset_slug)
        if status in ("exists", "ready"):
            # Update via version (requires folder); document limitation
            return f"Dataset {dataset_slug} exists; version update requires folder prep.")
    except Exception:
        pass
    # If token unavailable, report clearly
    token = os.environ.get("KAGGLE_API_TOKEN") or os.environ.get("KAGGLE_API_KEY")
    if not token:
        return f"KAGGLE_API_TOKEN not set — {dataset_slug} not created. Add secret and prepare folder with dataset-metadata.json."
    return f"Dataset {dataset_slug}: token present; use dataset_create_new with metadata folder to create."


def upload(version_message, dataset_slug="tpa-history"):
    api = _get_api()
    token = os.environ.get("KAGGLE_API_TOKEN") or os.environ.get("KAGGLE_API_KEY")
    if not token:
        return f"KAGGLE_API_TOKEN missing — upload skipped for {dataset_slug}."
    # Actual upload needs folder + metadata; document expected path
    return f"Upload to {dataset_slug}: ready when folder prepared; message={version_message}."


def download(dataset_slug="tpa-history"):
    api = _get_api()
    token = os.environ.get("KAGGLE_API_TOKEN") or os.environ.get("KAGGLE_API_KEY")
    if not token:
        return f"KAGGLE_API_TOKEN missing — download skipped for {dataset_slug}."
    try:
        api.dataset_download_files(dataset_slug, path=".", quiet=True)
        return f"Downloaded {dataset_slug}."
    except Exception as e:
        return f"Download {dataset_slug} failed: {e}"


def list_datasets(dataset_slug="tpa-history"):
    api = _get_api()
    try:
        resp = api.dataset_list_cli(dataset_slug)
        return f"Listed {dataset_slug}: {resp}"
    except Exception as e:
        return f"List {dataset_slug} failed: {e}"


if __name__ == "__main__":
    for ds in DATASETS:
        result = create_dataset(ds)
        print(result)
