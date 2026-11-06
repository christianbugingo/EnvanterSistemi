package security;
import product.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class QuantitiesTest {
    @Test void exactGramsParsingAndDisplay() {
        assertEquals(230,Quantities.parseGrams("0.23",false));assertEquals(230,Quantities.parseGrams("0.23 kg",false));
        assertEquals(230,Quantities.parseGrams("230 g",false));assertEquals(230,Quantities.parseGrams("230",true));
        assertEquals(230000,Quantities.parseGrams("230",false));assertEquals(1250,Quantities.parseGrams("1.250 kg",false));
        assertEquals(230,Quantities.parseGrams("0,23",false));assertEquals(230,Quantities.parseGrams(".23",false));
        assertEquals("230 g",Quantities.format(new BigDecimal("0.230"),Unit.KG));assertEquals("1.250 kg",Quantities.format(new BigDecimal("1.250"),Unit.KG));
        assertEquals("1.000 kg",Quantities.format(BigDecimal.ONE,Unit.KG));
        for(String bad:new String[]{"-1 g","0.0001 kg","1.2 g","NaN",""})assertThrows(IllegalArgumentException.class,()->Quantities.parseGrams(bad,false));
        assertThrows(IllegalArgumentException.class,()->Unit.PIECE.validate(new BigDecimal("1.5"),false));
    }
    @Test void halfUpLineRoundingUsesIntegerGrams() {
        assertEquals(new BigDecimal("11.50"),Quantities.total(230,new BigDecimal("49.99")));
        assertEquals(new BigDecimal("62.49"),Quantities.total(1250,new BigDecimal("49.99")));
        assertEquals(new BigDecimal("0.01"),Quantities.total(1,new BigDecimal("5.00")));
    }
    @Test void arbitraryWeightsUseCurrencyPrecisionAndEquivalentInputs() {
        var rwf=services.Currencies.get("RWF");var usd=services.Currencies.get("USD");var kwd=services.Currencies.get("KWD");BigDecimal price=new BigDecimal("100");
        assertEquals(200,Quantities.parseGrams("0.2 kg",false));
        assertEquals(new BigDecimal("20"),Quantities.total(Quantities.parseGrams("0.2 kg",false),price,rwf));
        assertEquals(new BigDecimal("23"),Quantities.total(Quantities.parseGrams("0.23 kg",false),price,rwf));
        assertEquals(Quantities.total(Quantities.parseGrams("230 g",false),price,rwf),Quantities.total(Quantities.parseGrams("0.23 kg",false),price,rwf));
        assertEquals(new BigDecimal("1"),Quantities.total(5,price,rwf));assertEquals(new BigDecimal("0.50"),Quantities.total(5,price,usd));assertEquals(new BigDecimal("0.500"),Quantities.total(5,price,kwd));
        assertEquals(new BigDecimal("150"),Quantities.total(Quantities.parseGrams("1.5 kg",false),price,rwf));assertEquals(230000,Quantities.parseGrams("230",false));
        assertEquals("200 g",Quantities.format(new BigDecimal("0.2"),Unit.KG));assertEquals("230 g",Quantities.format(new BigDecimal("0.23"),Unit.KG));assertEquals("1.250 kg",Quantities.format(new BigDecimal("1.250"),Unit.KG));
    }
    @Test void passwordPolicy() {
        for(String bad:new String[]{"short1","abcdefghijk","1234567890","USER123456","ü".repeat(40)+"1"})assertThrows(IllegalStateException.class,()->Passwords.validate("user123456",bad));
        assertDoesNotThrow(()->Passwords.validate("owner","GoodPassword123!"));
    }
    @Test void recoveryCodesAreRandomAndOnlyHashVerifies() {
        String code=Passwords.recoveryCode(),hash=Passwords.hash(code);assertNotEquals(code,Passwords.recoveryCode());
        assertFalse(hash.contains(code));assertTrue(hash.startsWith("$2a$12$"));assertTrue(Passwords.verify(code,hash));assertFalse(Passwords.verify("bad",hash));
    }
}
