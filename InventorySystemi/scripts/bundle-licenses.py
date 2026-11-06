"""Build-only license collection. No application runtime calls the network."""
from pathlib import Path
import urllib.request
import zipfile
root=Path(__file__).resolve().parents[2]
dest=root/'legal-licenses';dest.mkdir(exist_ok=True)
# Extract exact notices from the dependency artifacts already resolved for the build.
for folder,pattern in [('slf4j','**/slf4j-api-2.0.17.jar'),('junit','**/junit-jupiter-api-5.11.4.jar'),('opentest4j','**/opentest4j-1.3.0.jar'),('apiguardian','**/apiguardian-api-1.1.2.jar')]:
 p=next((root/'.tools/m2').glob(pattern));out=dest/folder;out.mkdir(exist_ok=True)
 with zipfile.ZipFile(p) as z:
  for n in z.namelist():
   if 'license' in n.lower() or 'notice' in n.lower():
    if not n.endswith('/'):(out/Path(n).name).write_bytes(z.read(n))
urls={
 'javafx/LICENSE.txt':'https://raw.githubusercontent.com/openjdk/jfx21u/master/LICENSE',
 'javafx/ADDITIONAL_LICENSE_INFO.txt':'https://raw.githubusercontent.com/openjdk/jfx21u/master/ADDITIONAL_LICENSE_INFO',
 'jbcrypt/LICENSE.txt':'https://raw.githubusercontent.com/jeremyh/jBCrypt/master/LICENSE',
 'h2/LICENSE.txt':'https://raw.githubusercontent.com/h2database/h2database/version-2.3.232/LICENSE.txt',
 'h2/MPL-2.0.txt':'https://www.mozilla.org/media/MPL/2.0/index.815ca599c9df.txt',
 'logback/LICENSE.txt':'https://raw.githubusercontent.com/qos-ch/logback/v_1.5.18/LICENSE.txt',
 'logback/EPL-1.0.html':'https://www.eclipse.org/legal/epl-v10.html',
 'apache/LICENSE.txt':'https://www.apache.org/licenses/LICENSE-2.0.txt',
}
failed=[]
for name,url in urls.items():
 path=dest/name;path.parent.mkdir(exist_ok=True)
 try:path.write_bytes(urllib.request.urlopen(url,timeout=30).read())
 except Exception as ex:failed.append((name,str(ex)))
print('License collection failures:',failed)
if failed:raise SystemExit(1)
