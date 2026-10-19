#!/usr/bin/env python3
import csv

rows = []
rows.append(['date','open','high','low','close','volume'])
base_date = 20230101
price = 110
vol = 10000
# Down to trough1
for i in range(10):
    price -= 0.8
    high = price + 0.4
    low = price - 1.0
    close = price - 0.6
    rows.append([str(base_date+i), f'{price+0.8:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+i*100)])
# Up
for i in range(6):
    price += 0.7
    high = price + 0.8
    low = price - 0.3
    close = price + 0.6
    rows.append([str(base_date+10+i), f'{price-0.7:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+1000+i*80)])
# Down to trough2
for i in range(8):
    price -= 0.75
    high = price + 0.3
    low = price - 0.9
    close = price - 0.5
    rows.append([str(base_date+16+i), f'{price+0.75:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+1500+i*90)])
# Up
for i in range(6):
    price += 0.7
    high = price + 0.8
    low = price - 0.3
    close = price + 0.6
    rows.append([str(base_date+24+i), f'{price-0.7:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+2200+i*80)])
# Down to trough3
for i in range(8):
    price -= 0.7
    high = price + 0.3
    low = price - 0.9
    close = price - 0.5
    rows.append([str(base_date+30+i), f'{price+0.7:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+2700+i*90)])
# Break up
for i in range(8):
    price += 1.0
    high = price + 1.2
    low = price - 0.3
    close = price + 0.9
    rows.append([str(base_date+38+i), f'{price-1.0:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+3400+i*120)])
with open('server/tests/fixtures/patterns/triple_bottom_pos.csv', 'w', newline='') as f:
    w = csv.writer(f)
    w.writerows(rows)
print(f'Wrote {len(rows)-1}')
