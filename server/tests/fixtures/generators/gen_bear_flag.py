#!/usr/bin/env python3
import csv

rows = []
rows.append(['date','open','high','low','close','volume'])
base_date = 20230101
price = 105
vol = 10000
# Pole down
pole_bars = 12
for i in range(pole_bars):
    price -= 0.9
    high = price + 0.5
    low = price - 1.0
    close = price - 0.7
    rows.append([str(base_date+i), f'{price+0.9:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+i*150)])
# Flag up
flag_bars = 8
for i in range(flag_bars):
    price += 0.3
    high = price + 0.5
    low = price - 0.4
    close = price + 0.2
    rows.append([str(base_date+pole_bars+i), f'{price-0.3:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+2000+i*80)])
# Breakout down
for i in range(5):
    price -= 1.0
    high = price + 0.3
    low = price - 1.2
    close = price - 0.9
    rows.append([str(base_date+pole_bars+flag_bars+i), f'{price+1.0:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+2800+i*200)])
with open('server/tests/fixtures/patterns/bear_flag_pos.csv', 'w', newline='') as f:
    w = csv.writer(f)
    w.writerows(rows)
print(f'Wrote {len(rows)-1}')
