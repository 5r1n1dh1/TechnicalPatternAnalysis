#!/usr/bin/env python3
import csv

# HS negative - shoulders differ significantly
rows = []
rows.append(['date','open','high','low','close','volume'])
base_date = 20230101
price = 100
vol = 10000
# build up
for i in range(15):
    price += 0.6
    high = price + 0.8
    low = price - 0.4
    close = price + 0.5
    rows.append([str(base_date+i), f'{price-0.6:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+i*100)])
# pullback
for i in range(5):
    price -= 0.5
    high = price + 0.3
    low = price - 0.8
    close = price - 0.5
    rows.append([str(base_date+15+i), f'{price+0.5:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+1500+i*80)])
# head
for i in range(10):
    price += 0.9
    high = price + 1.5
    low = price - 0.5
    close = price + 0.8
    rows.append([str(base_date+20+i), f'{price-0.9:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+1900+i*100)])
price = high
# pullback
for i in range(5):
    price -= 0.6
    high = price + 0.3
    low = price - 0.9
    close = price - 0.7
    rows.append([str(base_date+30+i), f'{price+0.6:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+2900+i*80)])
# right shoulder - much lower (diff > 15%)
for i in range(8):
    price += 0.3
    high = price + 0.6  # much lower than left shoulder
    low = price - 0.4
    close = price + 0.2
    rows.append([str(base_date+35+i), f'{price-0.3:.2f}', f'{high:.2f}', f'{low:.2f}', f'{close:.2f}', str(vol+3300+i*80)])
with open('server/tests/fixtures/patterns/hs_neg.csv', 'w', newline='') as f:
    w = csv.writer(f)
    w.writerows(rows)
print(f'Wrote {len(rows)-1}')
