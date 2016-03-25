package aerolito.magicmirror.module;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import aerolito.magicmirror.module.base.Module;

public class DateHelper extends Module {

    private static final String[] WEEKDAYS = new String[]{"", "Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"};
    private static final String[] SHORT_WEEKDAYS = new String[]{"", "Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};

    private SimpleDateFormat defaultDateFormat;
    private SimpleDateFormat forecastDateFormat;

    private static DateHelper instance = new DateHelper();

    public static DateHelper getInstance() {
        return instance;
    }

    private DateHelper() {
    }

    @Override
    public void init(Object... args) {
        super.init(args);
        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(Locale.getDefault());
        dateFormatSymbols.setWeekdays(WEEKDAYS);
        dateFormatSymbols.setShortWeekdays(SHORT_WEEKDAYS);
        this.defaultDateFormat = new SimpleDateFormat("EEEE', 'd ' de 'MMMM", Locale.getDefault());
        this.defaultDateFormat.setDateFormatSymbols(dateFormatSymbols);
        this.forecastDateFormat = new SimpleDateFormat("EE', 'd", Locale.getDefault());
        this.forecastDateFormat.setDateFormatSymbols(dateFormatSymbols);
    }

    protected String getModuleIdentifier() {
        return DateHelper.class.getName();
    }

    protected Object getProcessedResult(Object... args) {
        Calendar calendar = null;
        if (args.length > 0) {
            calendar = (Calendar) args[0];
        }
        if (calendar != null) {
            return forecastDateFormat.format(calendar.getTime());
        } else {
            return defaultDateFormat.format(Calendar.getInstance().getTime());
        }
    }
}
