package services;

import product.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Separate JVM = separate desktop session, used by the opt-in MySQL integration test. */
public final class RemoteCheckoutWorker {
    public static void main(String[] args) throws Exception {
        AuthService auth=new AuthService();
        try {
            var user=auth.login(args[0],"PrivateTest123!");
            if(args.length>1 && args[1].equals("wait")) {
                System.out.println("READY");System.out.flush();System.in.read();
            }
            Sale sale=new Sale(0,1,user.id(),new BigDecimal("3.000"),LocalDate.now());
            sale.setUnit(Unit.KG);sale.setPrice(new BigDecimal("49.99"));
            new SaleService().addMultipleSales(List.of(sale));System.out.println("COMMITTED");
        } catch(IllegalStateException e) {
            System.out.println("REJECTED: "+e.getMessage());System.exit(2);
        }
    }
}
