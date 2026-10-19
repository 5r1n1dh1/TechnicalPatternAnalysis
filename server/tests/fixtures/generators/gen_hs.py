#!/usr/bin/env python3
import csv

# Generate bearish head and shoulders
rows = []
rows.append(['date','open','high','low','close','volume'])
base_date = 20230101
# Start with uptrend
price = 100
vol = 10000
for i in range(20):  # build to left shoulder
    date = base_date + i
    high = price + 2
    low = price - 1
    close = price + 1
    rows.append([str(date), f'{price:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+i*100)])
    price += 0.5

# Left shoulder peak around here
ls_idx = len(rows)-1
# Pull back to form neckline
for i in range(5):
    date = base_date + 20 + i
    high = price + 0.5
    low = price - 1.5
    close = price - 1
    rows.append([str(date), f'{price:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+2000+i*50)])
    price -= 0.5  # down to neckline

t1_price = price  # first trough
# Head up
for i in range(10):
    date = base_date + 25 + i
    price += 1.0  # head rises
    high = price + 2.5  # head is higher
    low = price - 1
    close = price + 1
    rows.append([str(date), f'{price-1:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+3000+i*100)])
price = high  # head peak

# Pull back
for i in range(5):
    date = base_date + 35 + i
    high = price - 0.5
    low = price - 2
    close = price - 1.5
    rows.append([str(date), f'{price:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+4000+i*50)])
    price -= 0.5

t2_price = price  # second trough
# Right shoulder
for i in range(8):
    date = base_date + 40 + i
    price += 0.3  # rises slower
    high = price + 1.8  # similar to left shoulder (~102-103 level)
    low = price - 0.8
    close = price + 0.5
    rows.append([str(date), f'{price-0.3:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+4500+i*80)])
    price = high

# Breakout down through neckline
for i in range(5):
    date = base_date + 48 + i
    high = price - 0.5
    low = price - 2
    close = price - 1.8
    rows.append([str(date), f'{price:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+5000+i*100)])
    price -= 1.5

with open('server/tests/fixtures/patterns/hs_pos.csv', 'w', newline='') as f:
    w = csv.writer(f)
    w.writerows(rows)

print(f'Wrote {len(rows)-1} rows to hs_pos.csv')
