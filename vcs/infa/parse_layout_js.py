import re
txt = open(r'C:\Users\TKB_USER\Desktop\tkb\devops\vcs\backup\dist\assets\index.376ac911.js', encoding='utf-8', errors='ignore').read()

# menu items
for m in re.finditer(r'path:"/homepage"', txt):
    print('MENU:', txt[m.start()-80:m.start()+900])
    break

# pageTitle
for m in re.finditer(r'pageTitle', txt):
    print('\nPAGE:', txt[m.start():m.start()+500])
    break

# collapse
for kw in ['collapse-btn', 'const c=', 'c=K(', 'isCollapsed']:
    i = txt.find(kw)
    if i >= 0:
        print(f'\n{kw}:', txt[max(0,i-80):i+250])
