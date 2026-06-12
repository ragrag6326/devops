import re
from pathlib import Path

SRC = Path(r"c:\Users\TKB_USER\Downloads\index.76ac441a.css")
OUT = Path(__file__).parent / "extracted"

def extract_scope(css: str, sid: str) -> str:
    rules = []
    pattern = re.compile(r"([^{}]+)\[data-v-" + sid + r"\]\{([^}]+)\}")
    for m in pattern.finditer(css):
        sel = re.sub(r"\[data-v-[a-f0-9]+\]", "", m.group(1).strip())
        body = m.group(2).strip()
        decls = ";\n  ".join([d.strip() for d in body.split(";") if d.strip()])
        rules.append(f"{sel} {{\n  {decls};\n}}")
    return "\n\n".join(rules)

def main():
    css = SRC.read_text(encoding="utf-8")
    OUT.mkdir(exist_ok=True)
    scopes = {
        "homepage": "e39cfd78",
        "mr-review": "4d9a4954",
        "layout": "bea192e5",
        "log-analysis": "f1dbfd9b",
        "login": "d9dd3339",
        "monitor": "c4fbe3ef",
    }
    for name, sid in scopes.items():
        content = extract_scope(css, sid)
        path = OUT / f"{name}.css"
        path.write_text(content, encoding="utf-8")
        print(f"{name}: {content.count('{')} rules -> {path}")

if __name__ == "__main__":
    main()
