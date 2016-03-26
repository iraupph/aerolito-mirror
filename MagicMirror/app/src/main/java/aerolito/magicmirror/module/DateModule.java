package aerolito.magicmirror.module;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import aerolito.magicmirror.module.base.Module;

public class DateModule extends Module {

    private static final String[] WEEKDAYS = new String[]{"", "Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"};
    private static final String[] SHORT_WEEKDAYS = new String[]{"", "Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};

    private SimpleDateFormat defaultDateFormat;
    private SimpleDateFormat forecastDateFormat;

    private static DateModule instance = new DateModule();

    public static DateModule getInstance() {
        return instance;
    }

    private DateModule() {
    }

    @Override
    public void init(Object... args) {
        super.init(args);
        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(locale);
        dateFormatSymbols.setWeekdays(WEEKDAYS);
        dateFormatSymbols.setShortWeekdays(SHORT_WEEKDAYS);
        this.defaultDateFormat = new SimpleDateFormat("EEEE', 'd ' de 'MMMM", locale);
        this.defaultDateFormat.setDateFormatSymbols(dateFormatSymbols);
        this.forecastDateFormat = new SimpleDateFormat("EE', 'd", locale);
        this.forecastDateFormat.setDateFormatSymbols(dateFormatSymbols);
    }

    protected String getModuleIdentifier() {
        return DateModule.class.getName();
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
