import re, os, base64

js_path = r'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\backup\dist\assets\index.376ac911.js'
out_dir = r'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\web\src\assets'

with open(js_path, encoding='utf-8', errors='ignore') as f:
    js = f.read()

# Find vite asset imports like /assets/xxx-hash.gif
assets = set(re.findall(r'/assets/([a-zA-Z0-9_-]+\.(?:gif|png|jpg|jpeg|webp))', js))
print('asset paths:', assets)

# Find data URLs
data_urls = re.findall(r'data:image/(gif|png|jpeg|webp);base64,([A-Za-z0-9+/=]{100,})', js)
print('data url count:', len(data_urls))

# Find filename references
for name in ['1760253304510', 'swimming', 'koreafish', 'kp.png', 'liar', '昌']:
    idx = js.find(name)
    if idx >= 0:
        print(f'found {name} at {idx}:', js[max(0,idx-80):idx+120][:200])

# Extract base64 images near known filenames
for m in re.finditer(r'([a-zA-Z0-9_\-\u4e00-\u9fff]+\.(?:gif|png))', js):
    fn = m.group(1)
    if any(x in fn for x in ['swimming', 'koreafish', '176025', 'kp', 'liar', '昌']):
        print('filename ref:', fn)
