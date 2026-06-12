import re

css = open(r'c:\Users\TKB_USER\Downloads\index.76ac441a.css', encoding='utf-8').read()
sid = '7a57fd19'
rules = re.findall(r'[^{}]*\[data-v-' + sid + r'\][^{]*\{[^}]+\}', css)
open(r'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\infa\extracted\version-history.css', 'w', encoding='utf-8').write(
    '\n\n'.join(r.replace(f'[data-v-{sid}]', '').replace(f'.{sid}', '') for r in rules)
)

txt = open(r'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\backup\dist\assets\index.376ac911.js', encoding='utf-8', errors='ignore').read()
idx = txt.find('live-version-board')
start = txt.rfind('__name:"index"', 0, idx)
chunk = txt[start:idx + 8000]
open(r'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\infa\version_history_chunk.txt', 'w', encoding='utf-8').write(chunk)
