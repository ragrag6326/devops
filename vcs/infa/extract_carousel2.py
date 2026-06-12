import re, os

js_path = r'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\backup\dist\assets\index.411dc103.js'
with open(js_path, encoding='utf-8', errors='ignore') as f:
    js = f.read()

# Find all .gif/.png string literals
names = re.findall(r'"([^"]+\.(?:gif|png|jpg|jpeg|webp))"', js)
carousel_names = [n for n in names if any(x in n for x in ['176025', 'swimming', 'kp', '昌', 'liar', 'koreafish', 'bg.png'])]
print('carousel names:', carousel_names)

# Find vite import pattern: const X="url"
# Look around bgCaptions
idx = js.find('bgCaptions')
if idx >= 0:
    print('\n--- bgCaptions context ---')
    print(js[idx:idx+1500])

# Find asset URL patterns near image imports
for name in carousel_names:
    i = js.find(name)
    if i >= 0:
        chunk = js[max(0,i-200):i+len(name)+200]
        print(f'\n--- {name} ---')
        print(chunk)

# Extract all /assets/ references
assets = re.findall(r'"(/assets/[^"]+)"', js)
img_assets = [a for a in assets if any(ext in a for ext in ['.gif','.png','.jpg','.webp'])]
print('\nimg assets:', img_assets[:20])
