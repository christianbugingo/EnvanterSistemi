"""Local release replacement must preserve the prior signed build until all checks pass."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import finalize_macos_release as release

class ReleaseInstallTests(unittest.TestCase):
    def setUp(self):
        base=Path(__file__).resolve().parents[2]/'.tools/tmp';base.mkdir(parents=True,exist_ok=True)
        self.tmp=tempfile.TemporaryDirectory(dir=base,prefix='release-install-test-')
        self.project=Path(self.tmp.name)/'InventorySystemi';self.project.mkdir()
        self.override=patch.object(release,'PROJECT',self.project);self.override.start()
        self.stem='InventorySystem-1.3.0-macos-arm64'
        self.destination=self.project/'dist/macos';self.destination.mkdir(parents=True)
        (self.destination/(self.stem+'.dmg')).write_bytes(b'old notarized image')
        self.stage=self.project/'target/candidate';self.stage.mkdir(parents=True)
        (self.stage/(self.stem+'.app')).mkdir()
        (self.stage/(self.stem+'.app')/'content').write_text('new app')
        self.dmg=self.stage/(self.stem+'.dmg');self.dmg.write_bytes(b'new notarized image')
        self.evidence=self.project/'target/evidence';self.evidence.mkdir()
        self.submit={'status':'Accepted','id':'test-submission'}
        self.offline={'dmg_sha256':release.sha(self.dmg),'native_startup':True,'network_access_denied':True,'signed_bundle_modified':False,'gatekeeper_copied_app':'accepted'}
        self.write_proof()
    def write_proof(self):
        (self.evidence/'submission.json').write_text(json.dumps(self.submit))
        (self.evidence/'signed-offline.json').write_text(json.dumps(self.offline))
    def tearDown(self):self.override.stop();self.tmp.cleanup()
    def test_failed_checks_never_replace_existing_image(self):
        self.submit['status']='Invalid';self.write_proof()
        with self.assertRaises(ValueError):release.install(self.stage,self.evidence)
        self.assertEqual((self.destination/(self.stem+'.dmg')).read_bytes(),b'old notarized image')
    def test_verified_local_install_archives_old_image_and_preserves_other_platforms(self):
        windows=self.project/'dist/windows/old.zip';windows.parent.mkdir();windows.write_bytes(b'unchanged')
        result=release.install(self.stage,self.evidence)
        archived=Path(result['previous_release'])/(self.stem+'.dmg')
        self.assertEqual(archived.read_bytes(),b'old notarized image')
        self.assertEqual(windows.read_bytes(),b'unchanged')
        self.assertEqual((self.destination/(self.stem+'.dmg.sha256')).read_text(),release.sha(self.dmg if self.dmg.exists() else self.destination/self.dmg.name)+'  '+self.dmg.name+'\n')
        self.assertEqual(result['deleted_files'],[])
    def test_failed_install_move_rolls_back_to_previous_release(self):
        real_move=release.shutil.move
        def fail_candidate_image(source,destination):
            if Path(source)==self.dmg: raise OSError('Simulated local copy failure')
            return real_move(source,destination)
        with patch.object(release.shutil,'move',side_effect=fail_candidate_image):
            with self.assertRaises(OSError):release.install(self.stage,self.evidence)
        self.assertEqual((self.destination/(self.stem+'.dmg')).read_bytes(),b'old notarized image')
        self.assertTrue((self.stage/(self.stem+'.app')).exists())
    def test_modified_dmg_or_unverified_offline_run_refused(self):
        self.dmg.write_bytes(b'changed after verification')
        with self.assertRaises(ValueError):release.install(self.stage,self.evidence)
        self.offline['dmg_sha256']=release.sha(self.dmg);self.offline['native_startup']=False;self.write_proof()
        with self.assertRaises(ValueError):release.install(self.stage,self.evidence)
        self.assertEqual((self.destination/(self.stem+'.dmg')).read_bytes(),b'old notarized image')

if __name__=='__main__':unittest.main()
