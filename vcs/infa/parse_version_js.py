import re

for fname in ['index.376ac911.js', 'index.f1f67b04.js', 'index.7f972bf4.js']:
    path = rf'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\backup\dist\assets\{fname}'
    txt = open(path, encoding='utf-8', errors='ignore').read()
    idx = txt.find('live-version-board')
    if idx < 0:
        continue
    start = txt.rfind('__name:"index"', 0, idx)
    chunk = txt[start:idx + 12000]
    open(r'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\infa\version_history_chunk.txt', 'w', encoding='utf-8').write(chunk)
    print('found in', fname, 'start', start)
    break
