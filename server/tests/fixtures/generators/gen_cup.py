#!/usr/bin/env python3
import csv
import math

# Generate cup and handle
rows = []
rows.append(['date','open','high','low','close','volume'])
base_date = 20230101
price = 100
vol = 10000
# Left side up to rim
for i in range(10):
    date = base_date + i
    price += 0.5
    high = price + 1
    low = price - 0.5
    close = price + 0.3
    rows.append([str(date), f'{price-0.5:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+i*100)])

rim = price
# Cup formation - U shape
for i in range(1, 21):  # 20 bars down then up
    t = i / 20.0  # 0 to 1
    # Parabolic U: price decreases to middle then increases
    # depth ~15%
    depth = 0.15
    if i <= 10:
        cup_price = rim * (1 - depth * (i/10.0))  # going down
    else:
        cup_price = rim * (1 - depth * ((20-i)/10.0))  # going up
    date = base_date + 10 + i
    high = cup_price + 0.8
    low = cup_price - 1.2
    close = cup_price + 0.2
    rows.append([str(date), f'{cup_price-0.3:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+1000+i*150)])

# Handle - small pullback
handle_start = cup_price if 'cup_price' in locals() else rim*0.85
for i in range(8):  # handle 8 bars
    date = base_date + 30 + i
    price = rim * 0.9 + (i * 0.5)  # drifts up slowly, not below 1/3 depth? depth is 15%, so 1/3 is ~5% below rim
    # keep handle above rim - 0.05*rim
    if price < rim * 0.95:  # handle shallow
        price = rim * 0.95
    high = price + 0.6
    low = price - 0.5
    close = price + 0.4
    rows.append([str(date), f'{price-0.4:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+4000+i*80)])

# Breakout
for i in range(3):
    date = base_date + 38 + i
    price = rim * 1.02 + i*0.3
    high = price + 1.2
    low = price - 0.3
    close = price + 1.0
    rows.append([str(date), f'{price-1.0:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+5000+i*200)])

with open('server/tests/fixtures/patterns/cup_pos.csv', 'w', newline='') as f:
    w = csv.writer(f)
    w.writerows(rows)

print(f'Wrote {len(rows)-1} rows to cup_pos.csv')
