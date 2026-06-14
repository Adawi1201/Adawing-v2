"""提取 notes + messages 到桌面文本文件。"""
import re, os, sys
sys.stdout.reconfigure(encoding='utf-8')

dump = r"C:\Users\ASUL\Desktop\adawing-full-backup.sql"

with open(dump, "r", encoding="utf-8") as f:
    text = f.read()

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

def find_insert_block(text, table_name):
    """Find and return the full INSERT ... VALUES block for a table."""
    pattern = f"INSERT INTO `{table_name}`"
    pos = text.index(pattern)
    val_pos = text.index("VALUES ", pos) + len("VALUES ")
    # find end: );\n or just ;\n
    semi_pos = text.index(";\n", val_pos)
    block = text[val_pos:semi_pos].strip()
    return block

def split_rows(block):
    """Split a VALUES block into individual rows by ),(  """
    # remove leading ( and trailing ); or )
    block = block.strip()
    if block.startswith('('):
        block = block[1:]
    if block.endswith(');'):
        block = block[:-2]
    elif block.endswith(')'):
        block = block[:-1]
    return block.split('),(')

def parse_fields(s):
    fields = []
    remaining = s.strip()
    while remaining:
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

# ===== notes =====
outdir = r"C:\Users\ASUL\Desktop\adawing-articles"
os.makedirs(outdir, exist_ok=True)

block = find_insert_block(text, "note")
rows = split_rows(block)
print(f"Notes: {len(rows)} rows")

with open(os.path.join(outdir, "_notes.txt"), "w", encoding="utf-8") as f:
    for row in rows:
        fields = parse_fields(row)
        if len(fields) < 6:
            continue
        nid     = int(fields[0])
        title   = unquote(fields[1])
        content = unquote(fields[2])
        ntype   = "科技动态" if fields[3] == '1' else "个人动态"
        source  = fields[4] or "无"
        create  = fields[5]  # create_time is field 5
        # fields: id, title, content, type, source_id, create_time, update_time

        f.write(f"{'='*60}\n")
        f.write(f"ID: {nid}  |  类型: {ntype}  |  关联文章: {source}\n")
        f.write(f"时间: {create}\n")
        f.write(f"标题: {title}\n")
        f.write(f"{'='*60}\n")
        f.write(f"{content}\n\n")

print("  -> _notes.txt")

# ===== messages =====
block = find_insert_block(text, "message")
rows = split_rows(block)
print(f"Messages: {len(rows)} rows")

with open(os.path.join(outdir, "_messages.txt"), "w", encoding="utf-8") as f:
    for row in rows:
        fields = parse_fields(row)
        if len(fields) < 11:
            continue
        mid      = int(fields[0])
        nickname = unquote(fields[1])
        email    = unquote(fields[2])
        avatar   = fields[3] or "(无)"
        content  = unquote(fields[4])
        status   = {0:'待审核', 1:'已通过', 2:'已拒绝'}.get(int(fields[5]), str(fields[5]))
        reply    = unquote(fields[6]) if fields[6] else ""
        reject   = unquote(fields[7]) if fields[7] else ""
        likes    = fields[8]
        create   = fields[10]

        f.write(f"{'='*60}\n")
        f.write(f"ID: {mid}  |  {nickname}  |  {email}  |  点赞: {likes}\n")
        f.write(f"状态: {status}  |  时间: {create}\n")
        f.write(f"头像: {avatar}\n")
        f.write(f"内容: {content}\n")
        if reply:
            f.write(f"回复: {reply}\n")
        if reject:
            f.write(f"拒绝理由: {reject}\n")
        f.write("\n")

print("  -> _messages.txt")

# ===== system_config =====
block = find_insert_block(text, "system_config")
rows = split_rows(block)
print(f"System configs: {len(rows)} rows")

with open(os.path.join(outdir, "_system_config.txt"), "w", encoding="utf-8") as f:
    for row in rows:
        fields = parse_fields(row)
        if len(fields) < 5:
            continue
        key   = unquote(fields[1])
        value = unquote(fields[2]) if fields[2] else "(空)"
        desc  = unquote(fields[3]) if fields[3] else ""
        f.write(f"{key} = {value}\n")
        if desc:
            f.write(f"  # {desc}\n")

print("  -> _system_config.txt")
print(f"\nDone! All files in {outdir}")
