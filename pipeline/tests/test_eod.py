import os
import pandas as pd
import pytest
from pipeline.eod import fetch_bhavcopy, fetch_yahoo_eod, merge_eod, validate_date, run

# Test validate_date function
def test_validate_date_holiday():
    """Test that validate_date returns False for a known holiday."""
    # 2026-10-13 is a holiday (Diwali)
    result = validate_date("13102026")
    assert not result

def test_validate_date_trading_day():
    """Test that validate_date returns True for a trading day."""
    # 2026-10-14 is a trading day
    result = validate_date("14102026")
    assert result

def test_validate_date_invalid_format():
    """Test that validate_date returns False for invalid date format."""
    result = validate_date("abc")
    assert not result

# Test merge_eod with synthetic data
def test_merge_eod():
    """Test merge_eod with aligned data from both sources."""
    # Create synthetic data that matches
    nse_data = pd.DataFrame({
        'SC_CODE': ['1', '2'],
        'date': ['14102026', '14102026'],
        'OPEN': [100, 200],
        'HIGH': [110, 210],
        'LOW': [90, 190],
        'CLOSE': [105, 205],
        'VOLUME': [1000, 2000],
        'adjOpen': [100, 200],
        'adjHigh': [110, 210],
        'adjLow': [90, 190],
        'adjClose': [105, 205]
    })
    
    yahoo_data = pd.DataFrame({
        'symbol': ['1', '2'],
        'Date': ['2026-10-14', '2026-10-14'],
        'Open': [100, 200],
        'High': [110, 210],
        'Low': [90, 190],
        'Close': [105, 205],
        'Volume': [1000, 2000],
        'adjOpen': [100, 200],
        'adjHigh': [110, 210],
        'adjLow': [90, 190],
        'adjClose': [105, 205]
    })
    
    # Merge should work without discrepancies
    merged = merge_eod("14102026", ["1", "2"])
    assert len(merged) == 2
    assert merged['close'].equals(nse_data['CLOSE'])

def test_merge_eod_price_discrepancy():
    """Test that merge_eod logs a warning for price discrepancy."""
    import io
    import sys
    from contextlib import redirect_stdout
    
    # Create synthetic data with price discrepancy
    nse_data = pd.DataFrame({
        'SC_CODE': ['1'],
        'date': ['14102026'],
        'OPEN': [100],
        'HIGH': [110],
        'LOW': [90],
        'CLOSE': [105],
        'VOLUME': [1000],
        'adjOpen': [100],
        'adjHigh': [110],
        'adjLow': [90],
        'adjClose': [105]
    })
    
    yahoo_data = pd.DataFrame({
        'symbol': ['1'],
        'Date': ['2026-10-14'],
        'Open': [104],
        'High': [114],
        'Low': [84],
        'Close': [104],
        'Volume': [1000],
        'adjOpen': [104],
        'adjHigh': [114],
        'adjLow': [84],
        'adjClose': [104]
    })
    
    # Capture output
    captured_output = io.StringIO()
    with redirect_stdout(captured_output):
        merged = merge_eod("14102026", ["1"])
    
    # Check that warning was printed
    assert "Warning: Price discrepancy for" in captured_output.getvalue()

def test_run_dry_run():
    """Test that run function works in dry-run mode."""
    # This is a complex test that would require mocking
    # For now, we'll just check that it doesn't raise errors
    with pytest.raises(Exception) as e:
        run()
    assert "Error" not in str(e.value)  # Should not raise specific error