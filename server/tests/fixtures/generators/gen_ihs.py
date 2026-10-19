#!/usr/bin/env python3
import csv

# Generate inverse head and shoulders (bullish)
rows = []
rows.append(['date','open','high','low','close','volume'])
base_date = 20230101
price = 100
vol = 10000
# Downtrend to left shoulder
for i in range(15):
    date = base_date + i
    high = price + 0.8
    low = price - 1.5
    close = price - 1
    rows.append([str(date), f'{price:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+i*100)])
    price -= 0.7

# Left shoulder low
for i in range(3):  # bottom
    date = base_date + 15 + i
    high = price + 0.5
    low = price - 0.8
    close = price - 0.3
    rows.append([str(date), f'{price:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+1500+i*50)])
# Bounce up
for i in range(5):
    date = base_date + 18 + i
    price += 0.8
    high = price + 0.8
    low = price - 0.5
    close = price + 0.5
    rows.append([str(date), f'{price-0.8:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+1600+i*50)])

# Head lower
for i in range(10):
    date = base_date + 23 + i
    price -= 1.0
    high = price + 0.5
    low = price - 2.0  # head lower
    close = price - 1.0
    rows.append([str(date), f'{price+1:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+2000+i*100)])
price = low  # head low

# Bounce up
for i in range(5):
    date = base_date + 33 + i
    price += 0.6
    high = price + 0.8
    low = price - 0.4
    close = price + 0.5
    rows.append([str(date), f'{price-0.6:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+3000+i*50)])

# Right shoulder
for i in range(8):
    date = base_date + 38 + i
    price -= 0.4
    high = price + 0.4
    low = price - 0.6  # similar level to left shoulder
    close = price - 0.3
    rows.append([str(date), f'{price+0.4:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+3500+i*80)])
price = low

# Breakout up through neckline
for i in range(5):
    date = base_date + 46 + i
    price += 1.5
    high = price + 1.5
    low = price - 0.3
    close = price + 1.2
    rows.append([str(date), f'{price-1.5:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+4000+i*120)])

with open('server/tests/fixtures/patterns/ihs_pos.csv', 'w', newline='') as f:
    w = csv.writer(f)
    w.writerows(rows)

print(f'Wrote {len(rows)-1} rows to ihs_pos.csv')
