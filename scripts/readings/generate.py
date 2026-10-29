"""Generate offline reading aids, not accepted answers. No network access at generation time.

Install requirements.txt in a virtual environment first. Unknown kanji are omitted instead of guessed.
Term overrides are exact Sudachi-token matches; full-name overrides handle ambiguous compounds.
"""
from pathlib import Path
import argparse
import importlib.metadata
import json
import re
import unicodedata

ROOT = Path(__file__).resolve().parents[2]
READINGS = ROOT / 'src/main/resources/data/speakcraft/readings/ja.json'
SINGLE = dict(zip(
    'あいうえおかきくけこがぎぐげござじずぜぞさしすせそだぢづでどたちつてとなにぬねのはひふへほばびぶべぼぱぴぷぺぽまみむめもやゆよらりるれろわゐゑをんゔぁぃぅぇぉゃゅょゎ',
    'a i u e o ka ki ku ke ko ga gi gu ge go za ji zu ze zo sa shi su se so da ji zu de do ta chi tsu te to na ni nu ne no ha hi fu he ho ba bi bu be bo pa pi pu pe po ma mi mu me mo ya yu yo ra ri ru re ro wa i e o n vu a i u e o ya yu yo wa'.split(),
    strict=True))
PAIRS = {}
for head, prefix in {'き':'ky', 'ぎ':'gy', 'し':'sh', 'じ':'j', 'ち':'ch', 'ぢ':'j', 'に':'ny', 'ひ':'hy', 'び':'by', 'ぴ':'py', 'み':'my', 'り':'ry'}.items():
    for tail, vowel in {'ゃ':'a', 'ゅ':'u', 'ょ':'o'}.items():
        PAIRS[head + tail] = prefix + vowel
PAIRS.update(dict(zip(
    'いぇ うぃ うぇ うぉ しぇ じぇ ちぇ てぃ でぃ とぅ どぅ てゅ でゅ ふぁ ふぃ ふぇ ふぉ ふゅ ゔぁ ゔぃ ゔぇ ゔぉ ゔゅ くぁ くぃ くぇ くぉ ぐぁ すぃ ずぃ'.split(),
    'ye wi we wo she je che ti di tu du tyu dyu fa fi fe fo fyu va vi ve vo vyu kwa kwi kwe kwo gwa si zi'.split(), strict=True)))


def romanize(kana):
    text = unicodedata.normalize('NFKC', kana)
    text = ''.join(chr(ord(c) - 0x60) if 'ァ' <= c <= 'ヶ' else c for c in text)
    result = []
    geminate = False
    i = 0
    while i < len(text):
        char = text[i]
        if char == 'っ':
            if geminate: return None
            geminate = True
            i += 1
            continue
        if char == 'ー':
            if geminate or not result or result[-1][-1:] not in 'aeiou': return None
            result.append(result[-1][-1])
            i += 1
            continue
        pair = text[i:i+2]
        if pair in PAIRS:
            syllable = PAIRS[pair]
            i += 2
        elif char in SINGLE:
            syllable = SINGLE[char]
            i += 1
        elif char.isspace() or char in '・、。-':
            if geminate: return None
            result.append(' ')
            i += 1
            continue
        elif char.isascii() and (char.isalnum() or char in "'()"):
            syllable = char.lower()
            i += 1
        else:
            return None
        if geminate:
            if syllable[0] in 'aeioun': return None
            syllable = ('t' if syllable.startswith('ch') else syllable[0]) + syllable
            geminate = False
        if char == 'ん' and i < len(text) and text[i] in 'あいうえおやゆよ':
            syllable = "n'"
        result.append(syllable)
    return None if geminate else ' '.join(''.join(result).split())


def generate():
    from sudachipy import dictionary, tokenizer
    names = json.loads((ROOT / 'src/main/resources/data/speakcraft/names/ja.json').read_text(encoding='utf-8'))
    overrides = json.loads(Path(__file__).with_name('overrides.json').read_text(encoding='utf-8'))
    parser = dictionary.Dictionary(dict='core').create()
    entries, omitted, terms = {}, {}, {}
    for key, name in sorted(names.items()):
        if '%' in name:  # Format strings need runtime values, not a guessed reading.
            omitted[key] = name
            continue
        if name in overrides['names']:
            chunks = overrides['names'][name].split()
            source = 'override'
        else:
            chunks = []
            source = 'dictionary'
            for token in parser.tokenize(name, tokenizer.Tokenizer.SplitMode.C):
                surface = token.surface()
                kana = overrides['terms'].get(surface, token.reading_form())
                if surface in overrides['terms']:
                    source = 'dictionary+override'
                elif token.is_oov():
                    # Katakana loanwords may be OOV but their spelling specifies the reading.
                    if re.search(r'[一-龯々]', surface):
                        chunks = []
                        break
                    kana = surface
                terms[surface] = kana
                chunks.append(kana)
        # Sudachi can split an inflected verb after small tsu: クサッ + タ -> kusatta.
        joined = []
        for chunk in chunks:
            if joined and joined[-1].endswith(('ッ', 'っ')):
                joined[-1] += chunk
            else:
                joined.append(chunk)
        chunks = joined
        romanized = [romanize(chunk) for chunk in chunks]
        if not chunks or any(value is None for value in romanized):
            omitted[key] = name
            continue
        entries[key] = {'name': name, 'kana': ' '.join(chunks), 'romaji': ' '.join(value for value in romanized if value), 'source': source}
    report = {'generator': 'SudachiPy ' + importlib.metadata.version('SudachiPy'),
              'dictionary': 'SudachiDict-core ' + importlib.metadata.version('SudachiDict-core'),
              'entries': len(entries), 'omitted': omitted}
    return entries, report, terms


if __name__ == '__main__':
    cli = argparse.ArgumentParser()
    cli.add_argument('--check', action='store_true', help='Fail if committed readings differ from generated output')
    args = cli.parse_args()
    entries, report, terms = generate()
    text = json.dumps(entries, ensure_ascii=False, indent=2) + '\n'
    if args.check:
        if READINGS.read_text(encoding='utf-8') != text: raise SystemExit('Generated readings are stale')
    else:
        READINGS.parent.mkdir(parents=True, exist_ok=True)
        READINGS.write_text(text, encoding='utf-8')
        (ROOT / 'scripts/readings/coverage.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'entries': len(entries), 'omitted': len(report['omitted'])}))
