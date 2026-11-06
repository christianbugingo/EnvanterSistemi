"""Smoke-test the delivered Mac launcher/runtime on disposable data; never opens a real shop DB."""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile
import json
import argparse
import platform
from package_target import validate_native_host
from metadata import metadata

project = Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--arch', choices=('arm64','x86_64'), default='arm64')
args=parser.parse_args()
source = project / f"dist/macos/InventorySystem-{metadata()['version']}-macos-{args.arch}.app"
jdk = Path(os.environ['JAVA_HOME']) if os.environ.get('JAVA_HOME') else project.parent / '.tools/jdk/Contents/Home'
settings=subprocess.check_output([str(jdk/'bin/java'),'-XshowSettings:properties','-version'],stderr=subprocess.STDOUT,text=True)
jdk_arch=next(line.split('=',1)[1].strip() for line in settings.splitlines() if line.strip().startswith('os.arch ='))
validate_native_host('macos-'+args.arch,jdk_arch)

with tempfile.TemporaryDirectory(prefix='inventory-package-',dir=project.parent / '.tools/tmp') as temporary:
    work = Path(temporary)
    app = work / 'InventorySystem.app'
    shutil.copytree(source, app)
    contents = app / 'Contents'
    home = work / 'home'
    home.mkdir()
    env = os.environ.copy()
    env.update(DB_URL='jdbc:h2:file:' + str(home / 'startup/inventory') + ';DATABASE_TO_LOWER=TRUE', DB_USER='sa', DB_PASSWORD='')
    (home / 'startup').mkdir()
    startup_config=contents/'app/InventorySystem.cfg'
    startup_config.write_text(startup_config.read_text().replace('[JavaOptions]','[JavaOptions]\njava-options=-Duser.home='+str(home)+'\njava-options=-Djava.io.tmpdir='+str(work)))
    launcher = contents / 'MacOS/InventorySystem'
    command=['/usr/bin/sandbox-exec','-p','(version 1)(allow default)(deny network*)',str(launcher)]
    with (work / 'startup.log').open('w') as log:
        process = subprocess.Popen(command, env=env, stdout=log, stderr=log)
        try:
            process.wait(timeout=6)
            raise RuntimeError('Packaged app exited during startup: ' + (work / 'startup.log').read_text())
        except subprocess.TimeoutExpired:
            process.terminate()
            process.wait(timeout=10)
    startup = (work / 'startup.log').read_text()
    if 'Exception' in startup or 'ERROR' in startup:
        raise RuntimeError(startup)
    appdir = contents / 'app'
    libs = appdir / 'lib'
    jar = next(appdir.glob('inventory-system-*.jar'))
    java_source = work / 'PackagedSmoke.java'
    java_source.write_text(r'''
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import product.*;
import services.*;
import database.*;
public class PackagedSmoke {
  public static void main(String[] args) throws Exception {
    Path home=Path.of(System.getenv("INVENTORY_SMOKE_HOME"));
    System.setProperty("user.home",home.toString());
    System.setProperty("DB_URL","jdbc:h2:file:"+home.resolve("live/inventory")+";DATABASE_TO_LOWER=TRUE");
    System.setProperty("DB_USER","sa");System.setProperty("DB_PASSWORD","");Files.createDirectories(home.resolve("live"));
    DatabaseInitializer.initialize();AuthService auth=new AuthService();
    LegalService.accept();String recovery=auth.createFirstAdmin("testowner","DisposablePassword123!");
    if(recovery.isBlank())throw new AssertionError("Missing recovery code");auth.login("testowner","DisposablePassword123!");new ShopSettings().save("USD",BigDecimal.ZERO,"en");
    ProductService products=new ProductService();products.addProduct(new Product(0,"Packaged apples","Fruit",BigDecimal.ZERO,new BigDecimal("49.99"),"Test farm",Unit.KG,new BigDecimal("0.100"),0));
    new BatchService().receive(1,BigDecimal.ONE,new BigDecimal("30.00"),java.time.LocalDate.now(),null);
    Product p=products.loadProducts().getFirst();Sale s=new Sale(0,p.getProductId(),AuthService.current().id(),new BigDecimal("0.230"),java.time.LocalDate.now());s.setUnit(Unit.KG);s.setPrice(p.getPrice());
    Checkout checkout=new SaleService().recordSale(List.of(s),PaymentMethod.OTHER,UUID.randomUUID().toString());
    if(!checkout.total().equals(new BigDecimal("11.50")))throw new AssertionError("Wrong line total");
    Path receipt=utils.ReceiptGenerator.generateAndSaveReceipt(checkout);
    if(!Files.readString(receipt).contains("230 g"))throw new AssertionError("Wrong receipt weight");
    if(products.loadProducts().getFirst().getQuantity().compareTo(new BigDecimal("0.770"))!=0)throw new AssertionError("Wrong stock");
    var profit=new DashboardService().profit(ProfitPeriod.TODAY,java.time.LocalDate.now(),null,null,Currencies.get("USD"));
    if(profit.incomplete()||profit.grossProfit().compareTo(new BigDecimal("4.60"))!=0)throw new AssertionError("Wrong FIFO profit");
    Path backup=home.resolve("backup.zip");new BackupService().create(backup);
    org.h2.tools.Restore.execute(backup.toString(),home.resolve("restored").toString(),"inventory");
    System.setProperty("DB_URL","jdbc:h2:file:"+home.resolve("restored/inventory")+";DATABASE_TO_LOWER=TRUE");auth.login("testowner","DisposablePassword123!");new ShopSettings().save("USD",BigDecimal.ZERO,"en");
    if(products.loadProducts().getFirst().getQuantity().compareTo(new BigDecimal("0.770"))!=0)throw new AssertionError("Restore failed");
    if(new BatchService().load(1).getFirst().remaining()!=770)throw new AssertionError("Batch restore failed");
    auth.logout();Files.writeString(home.resolve("passed.txt"),"Native bundled runtime: setup, login, product, receive stock, weighted sale, FIFO profit, receipt, backup and batch restore passed.");
  }
}
''', encoding='utf-8')
    subprocess.run([str(jdk / 'bin/javac'), '--module-path', str(libs), '--add-modules', 'javafx.controls', '-cp', str(jar)+os.pathsep+str(libs / '*'), str(java_source)], check=True)
    smoke_jar = appdir / 'packaged-smoke.jar'
    subprocess.run([str(jdk / 'bin/jar'), '--create', '--file', str(smoke_jar), '-C', str(work), 'PackagedSmoke.class'], check=True)
    config = appdir / 'InventorySystem.cfg'
    config.write_text(config.read_text().replace('app.mainclass=main.Main', 'app.mainclass=PackagedSmoke') + '\n')
    text = config.read_text().replace('[Application]', '[Application]\napp.classpath=$APPDIR/packaged-smoke.jar')
    config.write_text(text)
    env['INVENTORY_SMOKE_HOME'] = str(home)
    result = subprocess.run(command, env=env, capture_output=True, text=True, timeout=45)
    if result.returncode or not (home / 'passed.txt').exists():
        raise RuntimeError(result.stdout + result.stderr + '\nPackaged business-workflow check failed')
    output = {'platform': 'macos-'+args.arch, 'native_startup': True, 'bundled_runtime_workflow': True, 'checks': (home / 'passed.txt').read_text(), 'real_shop_data_used': False, 'network_physically_disabled': False,'process_network_access_denied':True}
    destination = project / 'target/packaged-macos-verification.json'
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(output, indent=2)+'\n')
    print(json.dumps(output, indent=2))
