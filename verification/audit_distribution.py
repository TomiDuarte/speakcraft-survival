"""Inspect the built artifact without executing bundled libraries."""
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
import hashlib
import json

root = Path(__file__).resolve().parent.parent
properties = dict(line.split('=', 1) for line in (root / 'gradle.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
artifact = root / f"build/libs/speakcraft-survival-{properties['mod_version']}.jar"
with ZipFile(artifact) as archive:
    entries = archive.namelist()
    metadata = json.loads(archive.read('fabric.mod.json'))
    nested = [entry['file'] for entry in metadata['jars']]
    assert len(nested) == 2, nested
    natives = {}
    for filename in nested:
        with ZipFile(BytesIO(archive.read(filename))) as dependency:
            natives[filename] = [name for name in dependency.namelist() if name.endswith('.dll')]
    assert any('libvosk.dll' in name for names in natives.values() for name in names)
    assert any('jnidispatch.dll' in name for names in natives.values() for name in names)
    assert not any('gametest' in name.lower() or 'junit' in name.lower() or 'SurvivalGameTests' in name for name in entries)
    for language in ('es', 'de', 'ja'):
        assert len(json.loads(archive.read(f'data/speakcraft/names/{language}.json'))) > 2000
    assert len(json.loads(archive.read('data/speakcraft/readings/ja.json'))) > 2700
    assert 'META-INF/licenses/SudachiDict-LEGAL.txt' in entries
    assert 'THIRD_PARTY_NOTICES.md' in entries
report = {
    'artifact': artifact.relative_to(root).as_posix(),
    'bytes': artifact.stat().st_size,
    'sha256': hashlib.sha256(artifact.read_bytes()).hexdigest(),
    'version': metadata['version'],
    'dependencies': metadata['depends'],
    'nested_native_libraries': natives,
    'development_tests_excluded': True,
}
(root / 'verification/distribution.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
