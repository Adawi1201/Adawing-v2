"""从 v1 dump 提取文章为独立 Markdown 文件。

mysqldump 把整个 article INSERT 写成一行，\n 是转义字符而非真换行。
策略: 在行首按 ),( 分割，每个片段是一个文章行。
"""
import re, os, sys
sys.stdout.reconfigure(encoding='utf-8')

dump = r"C:\Users\ASUL\Desktop\adawing-full-backup.sql"
outdir = r"C:\Users\ASUL\Desktop\adawing-articles"
os.makedirs(outdir, exist_ok=True)

with open(dump, "r", encoding="utf-8") as f:
    text = f.read()

# ----- tags -----
tag_map = {}
# tag INSERT format: INSERT INTO `tag` (`id`, `name`, ...) VALUES (1,'name',...),(2,'name2',...)
tg_pos = text.index("INSERT INTO `tag`")
tg_end = text.index(";\n", tg_pos)
tg_block = text[tg_pos:tg_end]
for m in re.finditer(r"\((\d+),'([^']+)'", tg_block):
    tag_map[int(m.group(1))] = m.group(2)

art_tags = {}
# article_tag INSERT: VALUES (4,1,1),(17,2,2),...,(50,6,5);
# match all (aid?, tag_rel_id, article_id, tag_id) tuples
at_pos = text.index("INSERT INTO `article_tag`")
at_end = text.index(";\n", at_pos)
at_block = text[at_pos:at_end]
for m in re.finditer(r"\((\d+),(\d+),(\d+)\)", at_block):
    aid = int(m.group(2))
    tid = int(m.group(3))
    art_tags.setdefault(aid, []).append(tag_map.get(tid, f"tag_{tid}"))

# ----- articles: one huge line, split on ),( -----
pos = text.index("INSERT INTO `article`")
# everything from VALUES to the ;\n terminator
val_pos = text.index("VALUES (", pos) + len("VALUES ")
semi_pos = text.index(";\n", val_pos)
giant_line = text[val_pos:semi_pos].strip()

print(f"Giant line: {len(giant_line)} chars")

# Split on ),(
parts = giant_line.split('),(')

# parts[0] = (1,'Hi!Adawing!...   → strip leading (
# parts[1..N-1] = 2,'...  etc
# parts[N-1] = 6,'...最后有 trailing );

def strip_outer_parens(s):
    s = s.strip()
    if s.startswith('('):
        s = s[1:]
    if s.endswith(');'):
        s = s[:-2]
    elif s.endswith(')'):
        s = s[:-1]
    return s

parts[0] = strip_outer_parens(parts[0])
if parts:
    parts[-1] = strip_outer_parens(parts[-1])

print(f"Split into {len(parts)} parts")

def unquote(s):
    out = []
    i = 0
    while i < len(s):
        if s[i] == '\\' and i+1 < len(s):
            nxt = s[i+1]
            if nxt == 'n': out.append('\n')
            elif nxt == 't': out.append('\t')
            elif nxt == '\\': out.append('\\')
            elif nxt == "'": out.append("'")
            else: out.append(s[i:i+2])
            i += 2
        elif s[i] == "'" and i+1 < len(s) and s[i+1] == "'":
            out.append("'")
            i += 2
        else:
            out.append(s[i])
            i += 1
    return ''.join(out)

def parse_fields(s):
    """Parse comma-separated fields from string."""
    fields = []
    remaining = s.strip()

    while remaining and len(fields) < 12:
        remaining = remaining.lstrip()
        if not remaining:
            break
        if remaining[0] == ',':
            remaining = remaining[1:].lstrip()
            continue

        if remaining[0] == "'":
            end = 1
            while end < len(remaining):
                if remaining[end] == "'":
                    if end+1 < len(remaining) and remaining[end+1] == "'":
                        end += 2
                        continue
                    break
                elif remaining[end] == '\\':
                    end += 2
                    continue
                end += 1
            fields.append(remaining[1:end])
            remaining = remaining[end+1:]
        elif remaining[0].isdigit() or remaining[0] == '-':
            m = re.match(r'(-?\d+)', remaining)
            if m:
                fields.append(m.group(1))
                remaining = remaining[m.end():]
            else:
                remaining = remaining[1:]
        elif remaining.startswith('NULL'):
            fields.append(None)
            remaining = remaining[4:]
        else:
            remaining = remaining[1:]

    return fields

for p in parts:
    fields = parse_fields(p)
    if len(fields) < 12:
        preview = p[:120].replace('\n', '\\n')
        print(f"  WARN: {len(fields)} fields [{preview}...]")
        continue

    aid         = int(fields[0])
    title       = unquote(fields[1])
    summary     = unquote(fields[2])
    content     = unquote(fields[3])
    cover       = fields[4] or ""
    status_num  = int(fields[5])
    is_top      = int(fields[7])
    view_count  = int(fields[8])
    create_time = fields[10]
    update_time = fields[11]

    status = "PUBLISHED" if status_num == 1 else "DRAFT"
    tags = art_tags.get(aid, [])

    md = f"""# {title}

> **发布时间:** {create_time}  |  **阅读量:** {view_count}  |  **状态:** {status}  |  **置顶:** {"是" if is_top == 1 else "否"}
> **标签:** {", ".join(tags) if tags else "(无)"}
> **原封面:** {cover if cover else "(无)"}
> **摘要:** {summary}

---

{content}
"""
    safe_title = title[:50].replace('/', '-').replace(':', '-').replace('\n', ' ')
    safe_title = re.sub(r'[<>"|?*\\]', '', safe_title)
    fname = f"{aid:02d}-{safe_title}.md"
    path = os.path.join(outdir, fname)
    with open(path, "w", encoding="utf-8") as f:
        f.write(md)
    print(f"  OK  {fname}  tags=[{', '.join(tags)}]  views={view_count}  {create_time}")

print(f"\nDone! Output: {outdir}")
