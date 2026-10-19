#!/usr/bin/env python3
import csv

# Double top
rows = []
rows.append(['date','open','high','low','close','volume'])
base_date = 20230101
price = 90
vol = 10000
# Up to first peak
for i in range(15):
    price += 0.6
    high = price + 0.8
    low = price - 0.4
    close = price + 0.5
    rows.append([str(base_date+i), f'{price-0.6:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+i*100)])

peak1 = price
# Down to trough
for i in range(10):
    price -= 0.5
    high = price + 0.3
    low = price - 0.8
    close = price - 0.5
    rows.append([str(base_date+15+i), f'{price+0.5:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+1500+i*80)])

trough = price
# Up to second peak (similar to first)
for i in range(12):
    price += 0.55
    high = price + 0.8  # similar height
    low = price - 0.4
    close = price + 0.5
    rows.append([str(base_date+25+i), f'{price-0.55:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+2300+i*90)])

peak2 = price
# Break down
for i in range(8):
    price -= 0.8
    high = price + 0.2
    low = price - 1.0
    close = price - 0.7
    rows.append([str(base_date+37+i), f'{price+0.8:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+3400+i*120)])

with open('server/tests/fixtures/patterns/double_top_pos.csv', 'w', newline='') as f:
    w = csv.writer(f)
    w.writerows(rows)
print(f'Wrote {len(rows)-1} rows')
