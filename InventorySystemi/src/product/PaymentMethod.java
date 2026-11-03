package product;
import gui.LocaleManager;
public enum PaymentMethod {
    CASH, CARD, UNRECORDED;
    @Override public String toString() { return LocaleManager.get("payment."+name().toLowerCase(java.util.Locale.ROOT)); }
}
