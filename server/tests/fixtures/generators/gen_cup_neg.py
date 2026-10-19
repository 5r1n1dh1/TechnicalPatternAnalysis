#!/usr/bin/env python3
import csv

rows = []
rows.append(['date','open','high','low','close','volume'])
base_date = 20230101
price = 100
vol = 10000
# rim up
for i in range(8):
    price += 0.6
    high = price + 0.8
    low = price - 0.4
    close = price + 0.5
    rows.append([str(base_date+i), f'{price-0.6:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+i*100)])
rim = price
# cup
cup_depth = 0.15  # 15%
for i in range(1, 16):
    t = i/15.0
    if i <= 8:
        cup_price = rim * (1 - cup_depth*(i/8.0))
    else:
        cup_price = rim * (1 - cup_depth*((16-i)/8.0))
    date = base_date+8+i
    high = cup_price + 0.6
    low = cup_price - 0.8
    close = cup_price + 0.2
    rows.append([str(date), f'{cup_price-0.3:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+800+i*120)])
# handle too deep - goes to rim - 0.06*rim (more than 5% = 1/3 of 15%)
for i in range(6):
    price = rim * 0.93  # 7% below rim, cup depth 15% so 1/3 is 5%, so > 5% is bad
    date = base_date+23+i
    high = price + 0.5
    low = price - 0.6
    close = price + 0.1
    rows.append([str(date), f'{price:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+2500+i*80)])
with open('server/tests/fixtures/patterns/cup_neg.csv', 'w', newline='') as f:
    w = csv.writer(f)
    w.writerows(rows)
print(f'Wrote {len(rows)-1}')
