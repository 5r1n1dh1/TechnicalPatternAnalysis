#!/usr/bin/env python3
import csv

# Double negative - peaks differ significantly
rows = []
rows.append(['date','open','high','low','close','volume'])
base_date = 20230101
price = 90
vol = 10000
# up to first peak
for i in range(12):
    price += 0.7
    high = price + 0.9
    low = price - 0.4
    close = price + 0.6
    rows.append([str(base_date+i), f'{price-0.7:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+i*100)])
# down
for i in range(8):
    price -= 0.6
    high = price + 0.3
    low = price - 0.7
    close = price - 0.5
    rows.append([str(base_date+12+i), f'{price+0.6:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+1200+i*80)])
# up to second peak - much lower (diff > 10%)
for i in range(10):
    price += 0.4
    high = price + 0.5  # much lower than first
    low = price - 0.3
    close = price + 0.3
    rows.append([str(base_date+20+i), f'{price-0.4:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+2000+i*90)])
with open('server/tests/fixtures/patterns/double_neg.csv', 'w', newline='') as f:
    w = csv.writer(f)
    w.writerows(rows)
print(f'Wrote {len(rows)-1}')
