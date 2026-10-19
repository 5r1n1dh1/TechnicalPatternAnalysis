#!/usr/bin/env python3
import csv

rows = []
rows.append(['date','open','high','low','close','volume'])
base_date = 20230101
price = 95
vol = 10000
# pole up
for i in range(10):
    price += 0.9
    high = price + 1.0
    low = price - 0.5
    close = price + 0.7
    rows.append([str(base_date+i), f'{price-0.9:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+i*150)])
pole_end = price
# flag retracement too large (> half pole)
pole_height = 9.0  # approx
for i in range(8):
    price -= 0.6  # retraces more than half
    high = price + 0.5
    low = price - 0.6
    close = price - 0.4
    rows.append([str(base_date+10+i), f'{price+0.6:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+2000+i*80)])
with open('server/tests/fixtures/patterns/flag_neg.csv', 'w', newline='') as f:
    w = csv.writer(f)
    w.writerows(rows)
print(f'Wrote {len(rows)-1}')
