#!/usr/bin/env python3
import csv

# Double bottom
rows = []
rows.append(['date','open','high','low','close','volume'])
base_date = 20230101
price = 110
vol = 10000
# Down to first trough
for i in range(12):
    price -= 0.7
    high = price + 0.4
    low = price - 0.9
    close = price - 0.6
    rows.append([str(base_date+i), f'{price+0.7:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+i*100)])
# Up
for i in range(10):
    price += 0.6
    high = price + 0.8
    low = price - 0.3
    close = price + 0.5
    rows.append([str(base_date+12+i), f'{price-0.6:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+1200+i*80)])
# Down to second trough (similar)
for i in range(12):
    price -= 0.65
    high = price + 0.3
    low = price - 0.85
    close = price - 0.55
    rows.append([str(base_date+22+i), f'{price+0.65:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+2000+i*90)])
# Break up
for i in range(8):
    price += 0.9
    high = price + 1.0
    low = price - 0.3
    close = price + 0.8
    rows.append([str(base_date+34+i), f'{price-0.9:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+3100+i*120)])
with open('server/tests/fixtures/patterns/double_bottom_pos.csv', 'w', newline='') as f:
    w = csv.writer(f)
    w.writerows(rows)
print(f'Wrote {len(rows)-1}')
