package aerolito.magicmirror.module;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateHelper {

    private static final String[] WEEKDAYS = new String[]{"", "Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"};
    private static final String[] SHORT_WEEKDAYS = new String[]{"", "Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};

    private SimpleDateFormat defaultDateFormat;
    private SimpleDateFormat forecastDateFormat;

    private static DateHelper instance = new DateHelper();

    public static DateHelper getInstance() {
        return instance;
    }
    
    public DateHelper() {
        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(Locale.getDefault());
        dateFormatSymbols.setWeekdays(WEEKDAYS);
        dateFormatSymbols.setShortWeekdays(SHORT_WEEKDAYS);
        this.defaultDateFormat = new SimpleDateFormat("EEEE', 'd ' de 'MMMM", Locale.getDefault());
        this.defaultDateFormat.setDateFormatSymbols(dateFormatSymbols);
        this.forecastDateFormat = new SimpleDateFormat("EE', 'd", Locale.getDefault());
        this.forecastDateFormat.setDateFormatSymbols(dateFormatSymbols);
    }

    public String getDate() {
        return defaultDateFormat.format(Calendar.getInstance().getTime());
    }

    public String getForecastDate(Calendar calendar) {
        return forecastDateFormat.format(calendar.getTime());
    }
}
