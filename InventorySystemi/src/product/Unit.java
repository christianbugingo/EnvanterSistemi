package product;

import gui.LocaleManager;
import java.math.BigDecimal;

public enum Unit {
    PIECE, KG;
    public String label() { return LocaleManager.get("unit." + name().toLowerCase(java.util.Locale.ROOT)); }
    @Override public String toString() { return label(); }
    public BigDecimal validate(BigDecimal quantity, boolean allowZero) {
        if (quantity == null || quantity.signum() < 0 || (!allowZero && quantity.signum() == 0)
                || quantity.compareTo(new BigDecimal("99999999999")) > 0
                || quantity.stripTrailingZeros().scale() > (this == PIECE ? 0 : 3)) {
            throw new IllegalArgumentException(LocaleManager.get(this == PIECE ? "validation.pieces" : "validation.weight"));
        }
        return quantity.setScale(3);
    }
}
