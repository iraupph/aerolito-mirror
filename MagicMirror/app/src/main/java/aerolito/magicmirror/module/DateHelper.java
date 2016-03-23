package aerolito.magicmirror.module;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateHelper {

    private static final String[] WEEKDAYS = new String[]{"", "Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"};

    private SimpleDateFormat dateFormat;

    private static DateHelper instance = new DateHelper();

    public static DateHelper getInstance() {
        return instance;
    }

    public DateHelper() {
        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(Locale.getDefault());
        dateFormatSymbols.setWeekdays(WEEKDAYS);
        this.dateFormat = new SimpleDateFormat("EEEE', 'd ' de 'MMMM", Locale.getDefault());
        this.dateFormat.setDateFormatSymbols(dateFormatSymbols);
    }

    public String getDate() {
        return dateFormat.format(Calendar.getInstance().getTime());
    }
}
