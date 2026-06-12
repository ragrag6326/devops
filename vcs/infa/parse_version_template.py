import re
txt = open(r'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\infa\version_history_chunk.txt', encoding='utf-8').read()
# find render return after pfe setup
idx = txt.find('return(w(),T(Fe,null,')
if idx < 0:
    idx = txt.find('(m,p)=>(w(),T(Fe,null,')
chunk = txt[idx:idx+15000] if idx >= 0 else txt[-15000:]
open(r'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\infa\version_template.txt', 'w', encoding='utf-8').write(chunk)
