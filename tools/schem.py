"""Reads a Sponge schematic (.schem): its size, palette and blocks."""
import gzip, struct, sys, collections
def nbt(b, i, t):
    if t == 1: return struct.unpack_from('>b', b, i)[0], i + 1
    if t == 2: return struct.unpack_from('>h', b, i)[0], i + 2
    if t == 3: return struct.unpack_from('>i', b, i)[0], i + 4
    if t == 4: return struct.unpack_from('>q', b, i)[0], i + 8
    if t == 5: return struct.unpack_from('>f', b, i)[0], i + 4
    if t == 6: return struct.unpack_from('>d', b, i)[0], i + 8
    if t == 7:
        n = struct.unpack_from('>i', b, i)[0]; return bytes(b[i + 4:i + 4 + n]), i + 4 + n
    if t == 8:
        n = struct.unpack_from('>H', b, i)[0]; return b[i + 2:i + 2 + n].decode('utf-8', 'replace'), i + 2 + n
    if t == 9:
        et = b[i]; n = struct.unpack_from('>i', b, i + 1)[0]; i += 5; out = []
        for _ in range(n): v, i = nbt(b, i, et); out.append(v)
        return out, i
    if t == 10:
        d = {}
        while True:
            tt = b[i]; i += 1
            if tt == 0: return d, i
            n = struct.unpack_from('>H', b, i)[0]; name = b[i + 2:i + 2 + n].decode('utf-8', 'replace'); i += 2 + n
            v, i = nbt(b, i, tt); d[name] = v
    if t == 11:
        n = struct.unpack_from('>i', b, i)[0]; return list(struct.unpack_from('>%di' % n, b, i + 4)), i + 4 + 4 * n
    if t == 12:
        n = struct.unpack_from('>i', b, i)[0]; return list(struct.unpack_from('>%dq' % n, b, i + 4)), i + 4 + 8 * n
    raise ValueError(t)
def load(path):
    raw = gzip.open(path).read()
    t = raw[0]; n = struct.unpack_from('>H', raw, 1)[0]
    root, _ = nbt(raw, 3 + n, t)
    return root
def varints(b):
    out = []; i = 0
    while i < len(b):
        v = 0; s = 0
        while True:
            x = b[i]; i += 1; v |= (x & 0x7F) << s; s += 7
            if not x & 0x80: break
        out.append(v)
    return out
def blocks(root):
    r = root.get('Schematic', root)
    W, H, L = r['Width'], r['Height'], r['Length']
    bl = r.get('Blocks', r)
    pal = bl.get('Palette', r.get('Palette'))
    data = bl.get('Data', r.get('BlockData'))
    inv = {v: k for k, v in pal.items()}
    ids = varints(data)
    return W, H, L, inv, ids, bl.get('BlockEntities', r.get('BlockEntities', [])), r
if __name__ == '__main__':
    root = load(sys.argv[1])
    r = root.get('Schematic', root)
    print('keys', list(r.keys()), 'version', r.get('Version'), 'data', r.get('DataVersion'))
    W, H, L, inv, ids, bes, r = blocks(root)
    print('size', W, H, L, 'offset', r.get('Offset'), 'meta', r.get('Metadata'))
    c = collections.Counter(inv[i].split('[')[0] for i in ids)
    print(len(ids), c.most_common(60))
    print('block entities', len(bes), collections.Counter(be.get('Id') for be in bes))
