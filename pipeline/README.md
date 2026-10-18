# Daily EOD Data Pipeline

This repository contains a pipeline for ingesting and publishing daily equity data from NSE and Yahoo Finance.

## Features
- Fetches NSE Bhavcopy data via API
- Merges with Yahoo Finance data
- Validates trading days against NSE calendar
- Publishes to Kaggle dataset
- Includes backfill script for historical data

## Usage
1. Run the daily pipeline: `gh workflow run eod.yml`
2. Run the backfill script: `python pipeline/backfill.py`

## Workflow
- Scheduled daily at 19:00 IST (Mon-Fri)
- Validates trading day first
- Fetches data for all 500 NSE symbols
- Merges and validates data
- Uploads to Kaggle dataset
- Deploys to Vercel

## Backfill
- One-time script to load 10 years of historical data
- Uses Yahoo Finance with adjusted prices
- Uploads to Kaggle as a versioned dataset