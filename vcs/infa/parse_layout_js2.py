import re
txt = open(r'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\backup\dist\assets\index.376ac911.js', encoding='utf-8', errors='ignore').read()
idx = txt.find('class:"app-shell"')
# find render function start
start = txt.rfind('const Jve', 0, idx)
if start < 0:
    start = txt.rfind('Jve=', 0, idx)
print('start', start)
chunk = txt[start:start+8000]
# decode unicode escapes for readability
chunk2 = chunk.encode().decode('unicode_escape')
open(r'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\infa\layout_chunk.txt', 'w', encoding='utf-8').write(chunk2[:8000])
