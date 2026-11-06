"""Build-tool regressions; no app startup and no access to shop data."""
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import struct
import zipfile
import package_target as pt

class PackagingTests(unittest.TestCase):
    def setUp(self):
        root = Path(__file__).resolve().parents[2]
        (root / '.tools/tmp').mkdir(parents=True, exist_ok=True)
        self.temporary = tempfile.TemporaryDirectory(dir=root / '.tools/tmp', prefix='packaging-test-')
        self.root = Path(self.temporary.name)
        self.project = self.root / 'InventorySystemi'
        self.project.mkdir()
        self.override = patch.object(pt, 'PROJECT', self.project)
        self.override.start()
    def tearDown(self):
        self.override.stop()
        self.temporary.cleanup()
    def test_rosetta_is_not_intel_hardware(self):
        from types import SimpleNamespace
        with patch.object(pt.platform, 'system', return_value='Darwin'), patch.object(pt.subprocess, 'run', return_value=SimpleNamespace(stdout='1\n')):
            with self.assertRaisesRegex(ValueError, 'Rosetta'): pt.validate_native_host('macos-x86_64', 'x86_64')
    def test_native_hosts_and_jdks_only(self):
        for target, values in pt.TARGETS.items():
            system, machine, _ = values
            pt.validate_host(target, system, machine, machine)
            for wrong in [('Darwin', 'arm64', 'x86_64'), ('Darwin', 'x86_64', 'arm64'), ('Linux', 'aarch64', 'amd64'), ('Windows', 'amd64', 'amd64')]:
                with self.assertRaises(ValueError): pt.validate_host(target, *wrong)
    def test_same_target_cleanup_preserves_other_outputs(self):
        own = self.project / 'target/build-macos-arm64'
        other = self.project / 'target/build-macos-x86_64/keep'
        other.parent.mkdir(parents=True); other.write_text('intel')
        own.mkdir(); (own / 'old').write_text('old')
        pt.prepare(own)
        self.assertEqual(other.read_text(), 'intel')
        self.assertEqual(list(own.iterdir()), [])
        with self.assertRaises(ValueError): pt.prepare(self.project / 'target')
        with self.assertRaises(ValueError): pt.prepare(self.root / 'outside')
    def test_database_and_symlink_cleanup_refused(self):
        own = self.project / 'target/build-linux-x86_64'; own.mkdir(parents=True)
        (own / 'inventory.mv.db').write_text('protected')
        with self.assertRaises(ValueError): pt.prepare(own)
        link = self.project / 'target/link'; link.symlink_to(self.root, target_is_directory=True)
        with self.assertRaises(ValueError): pt.prepare(link)
    def test_publication_preserves_other_arch_and_windows(self):
        destination = self.project / 'dist/macos'; destination.mkdir(parents=True)
        other = destination / 'InventorySystem-1.3.0-macos-x86_64.dmg'; other.write_text('intel')
        windows = self.project / 'dist/windows/keep.zip'; windows.parent.mkdir(); windows.write_text('windows')
        old = destination / 'InventorySystem-1.2.1-macos-arm64.dmg'; old.write_text('old arm')
        stage = self.project / 'target/release-macos-arm64'; stage.mkdir(parents=True)
        (stage / 'InventorySystem-1.3.0-macos-arm64.app').mkdir()
        (stage / 'InventorySystem-1.3.0-macos-arm64.dmg').write_text('new arm')
        pt.publish('macos-arm64', stage)
        self.assertFalse(old.exists()); self.assertEqual(other.read_text(), 'intel'); self.assertEqual(windows.read_text(), 'windows')
    def test_incomplete_release_keeps_previous(self):
        old = self.project / 'dist/linux/InventorySystem-1.2.1-linux-x86_64.tar.gz'; old.parent.mkdir(parents=True); old.write_text('previous')
        stage = self.project / 'target/release-linux-x86_64'; stage.mkdir(parents=True)
        with self.assertRaises(ValueError): pt.publish('linux-x86_64', stage)
        self.assertEqual(old.read_text(), 'previous')
    def test_foreign_native_inside_jar_rejected(self):
        image = self.project / 'target/image'; image.mkdir(parents=True)
        with zipfile.ZipFile(image / 'wrong.jar', 'w') as archive: archive.writestr('wrong.dll', b'MZ' + bytes(62))
        with self.assertRaisesRegex(ValueError, 'Wrong native'): pt.verify_natives(image, 'macos-arm64')
    def test_classifier_and_runtime_checks(self):
        (self.project / 'pom.xml').write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><properties><javafx.version>21.0.6</javafx.version></properties></project>')
        image = self.project / 'target/input'; image.mkdir(parents=True)
        native = b'\xcf\xfa\xed\xfe' + struct.pack('<I', 0x100000c) + bytes(56)
        for component in ('base', 'graphics', 'controls'):
            with zipfile.ZipFile(image / f'javafx-{component}-21.0.6-mac-aarch64.jar', 'w') as archive:
                if component == 'graphics': archive.writestr('libglass.dylib', native)
        self.assertEqual(len(pt.verify_natives(image, 'macos-arm64')), 1)
        with zipfile.ZipFile(image / 'javafx-base-21.0.6-linux.jar', 'w'): pass
        with self.assertRaisesRegex(ValueError, 'Unexpected JavaFX classifier'): pt.verify_natives(image, 'macos-arm64')
        (image / 'javafx-base-21.0.6-linux.jar').unlink()
        (image / 'wrong-runtime').write_bytes(b'\x7fELF\x02\x01' + bytes(58))
        with self.assertRaisesRegex(ValueError, 'Wrong executable/library'): pt.verify_natives(image, 'macos-arm64')
    def test_binary_headers(self):
        self.assertEqual(pt.binary_kind(b'\xcf\xfa\xed\xfe' + struct.pack('<I', 0x1000007) + bytes(56)), ('Darwin', 'x86_64'))
        self.assertEqual(pt.binary_kind(b'\xcf\xfa\xed\xfe' + struct.pack('<I', 0x100000c) + bytes(56)), ('Darwin', 'arm64'))
        elf = bytearray(64); elf[:6] = b'\x7fELF\x02\x01'; struct.pack_into('<H', elf, 18, 62)
        self.assertEqual(pt.binary_kind(elf), ('Linux', 'x86_64'))

if __name__ == '__main__': unittest.main()
