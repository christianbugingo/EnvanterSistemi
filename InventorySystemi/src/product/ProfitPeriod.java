package product;
import gui.LocaleManager;
import java.time.*;
public enum ProfitPeriod {
    TODAY,WEEK,MONTH,YEAR,CUSTOM;
    public record Range(LocalDate start,LocalDate end) { }
    public Range range(LocalDate today,LocalDate start,LocalDate end){
        return switch(this){
            case TODAY->new Range(today,today);
            case WEEK->new Range(today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),today);
            case MONTH->new Range(today.withDayOfMonth(1),today);
            case YEAR->new Range(today.withDayOfYear(1),today);
            case CUSTOM->{if(start==null||end==null||start.isAfter(end))throw services.AuthService.failure("msg.start.after.end");yield new Range(start,end);}
        };
    }
    @Override public String toString(){return LocaleManager.get("period."+name().toLowerCase(java.util.Locale.ROOT));}
}
