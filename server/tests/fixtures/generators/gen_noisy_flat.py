#!/usr/bin/env python3
import csv
import random

random.seed(42)
rows = []
rows.append(['date','open','high','low','close','volume'])
base_date = 20230101
price = 100
vol = 10000
for i in range(60):
    change = random.gauss(0, 0.5)
    open_p = price
    close_p = price + change
    high_p = max(open_p, close_p) + abs(random.gauss(0, 0.3))
    low_p = min(open_p, close_p) - abs(random.gauss(0, 0.3))
    rows.append([str(base_date+i), f'{open_p:.2f}', f'{high_p:.2f}', f'{low_p:.2f}', f'{close_p:.2f}', str(vol+i*10)])
    price = close_p
with open('server/tests/fixtures/patterns/noisy_flat.csv', 'w', newline='') as f:
    w = csv.writer(f)
    w.writerows(rows)
print(f'Wrote {len(rows)-1}')
