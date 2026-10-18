# Kaggle dataset operations

import os
import subprocess
from datetime import datetime

def create_dataset():
    """Create a new Kaggle dataset from the history.parquet file."""
    subprocess.run([
        "kaggle", "datasets", "create", 
        "-d", "tpa-history", 
        "-p", "history.parquet"
    ], check=True, env={"KAGGLE_API_KEY": os.environ.get("KAGGLE_API_KEY")})

def upload(version_message):
    """Upload the dataset with a version tag."""
    subprocess.run([
        "kaggle", "datasets", "version", 
        "-p", "tpa-history", 
        "-m", f"{version_message}"
    ], check=True, env={"KAGGLE_API_KEY": os.environ.get("KAGGLE_API_KEY")})

def download():
    """Download the dataset to history.parquet."""
    subprocess.run([
        "kaggle", "datasets", "download", 
        "-d", "tpa-history", 
        "-p", "history.parquet"
    ], check=True, env={"KAGGLE_API_KEY": os.environ.get("KAGGLE_API_KEY")})

def list_datasets():
    """List all datasets for the user."""
    subprocess.run([
        "kaggle", "datasets", "list", 
        "-d", "tpa-history"
    ], check=True, env={"KAGGLE_API_KEY": os.environ.get("KAGGLE_API_KEY")})