#!/usr/bin/env python3
"""Build reproducible, dependency-free source downloads; never include dev saves."""

import argparse
import hashlib
import io
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PROJECTS = {
    "json-kubejs-addon": "divinity-json-kubejs-starter",
    "java-addon": "divinity-java-starter",
}
EXCLUDED = {".git", ".gradle", ".idea", "build", "libs", "run", "run-server", "node_modules", "__pycache__"}


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def package(source_name, archive_name):
    source = ROOT / "templates" / source_name
    if not (source / "README.md").is_file():
        raise ValueError(f"Missing starter README: {source}")
    files = []
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for file in sorted(source.rglob("*")):
            relative = file.relative_to(source)
            if any(part in EXCLUDED for part in relative.parts):
                continue
            if file.name == ".DS_Store" or file.suffix == ".iml":
                continue
            if file.is_symlink():
                raise ValueError(f"Do not ship symlinks: {file}")
            if not file.is_file():
                continue
            if file.suffix in {".jar", ".class"} and relative.as_posix() != "gradle/wrapper/gradle-wrapper.jar":
                raise ValueError(f"Unexpected binary in source template: {file}")
            if file.name.startswith(".env") or file.suffix in {".key", ".pem", ".p12"}:
                raise ValueError(f"Unexpected credentials file: {file}")
            data = file.read_bytes()
            if file.suffix == ".json" or file.name == "pack.mcmeta":
                json.loads(data)
            entry = zipfile.ZipInfo(f"{archive_name}/{relative.as_posix()}", (2026, 1, 1, 0, 0, 0))
            entry.create_system = 3
            entry.external_attr = (0o100755 if file.name == "gradlew" else 0o100644) << 16
            entry.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(entry, data, compresslevel=9)
            files.append({"path": relative.as_posix(), "bytes": len(data), "sha256": sha256(data)})
    data = buffer.getvalue()
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        if archive.testzip() is not None:
            raise ValueError(f"Corrupt archive: {archive_name}")
    return data, {"archive": archive_name + ".zip", "source": f"templates/{source_name}",
                  "bytes": len(data), "sha256": sha256(data), "files": files}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Fail if published downloads differ from source")
    options = parser.parse_args()
    outputs = {}
    manifests = []
    for source, archive in PROJECTS.items():
        data, manifest = package(source, archive)
        outputs[archive + ".zip"] = data
        manifests.append(manifest)
    outputs["addon-templates.json"] = (json.dumps({"format": 1, "templates": manifests}, indent=2) + "\n").encode()
    destination = ROOT / "downloads"
    if not options.check:
        destination.mkdir(exist_ok=True)
    for name, data in outputs.items():
        target = destination / name
        if options.check:
            if not target.is_file() or target.read_bytes() != data:
                raise SystemExit(f"Download is stale: {name}; run python3 tools/package_addon_templates.py")
        else:
            target.write_bytes(data)
    print(("Verified" if options.check else "Packaged") + " 2 starter archives and their SHA-256/file inventory.")


if __name__ == "__main__":
    main()
