import re

txt = open(r'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\backup\dist\assets\index.376ac911.js', encoding='utf-8', errors='ignore').read()
idx = txt.find('login-page')
start = txt.rfind('__name:"index"', 0, idx)
chunk = txt[start:idx + 5000]
open(r'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\infa\login_chunk.txt', 'w', encoding='utf-8').write(chunk)

css = open(r'c:\Users\TKB_USER\Downloads\index.76ac441a.css', encoding='utf-8').read()
for m in re.finditer(r'@keyframes [^{]+d9dd3339\{[^}]+\}[^@]*', css):
    open(r'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\infa\login_animations.txt', 'a', encoding='utf-8').write(m.group() + '\n\n')
