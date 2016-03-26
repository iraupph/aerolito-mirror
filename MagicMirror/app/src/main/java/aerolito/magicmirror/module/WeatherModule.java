package aerolito.magicmirror.module;

import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import aerolito.magicmirror.BuildConfig;
import aerolito.magicmirror.R;
import aerolito.magicmirror.model.OpenWeatherResponse;
import aerolito.magicmirror.module.base.Module;
import aerolito.magicmirror.util.L;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class WeatherModule extends Module {

    public static final String OPEN_WEATHER_API_KEY = "652e744640b3afa9dfbd94921c24399c";
    public static final String OPEN_WEATHER_API_URL = "http://api.openweathermap.org/data/2.5/";

    public static final String UNIT_METRIC = "metric";

    protected static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
    private static HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    private static Retrofit.Builder retrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(OPEN_WEATHER_API_URL);

    private HashMap<String, Integer> forecastsMapping;
    private HashMap<String, String> isoCountryMapping;
    private DateModule dateModule = DateModule.getInstance();

    private static WeatherModule instance = new WeatherModule();

    public static WeatherModule getInstance() {
        return instance;
    }

    private WeatherModule() {
    }

    @Override
    public void init(L logger, Object... args) {
        super.init(logger, args);

        this.forecastsMapping = new HashMap<>();
        this.forecastsMapping.put("Clouds", R.drawable.vc_weather_cloud);
        this.forecastsMapping.put("Thunderstorm", R.drawable.vc_weather_thunderstorm);
        this.forecastsMapping.put("Drizzle", R.drawable.vc_weather_drizzle);
        this.forecastsMapping.put("Rain", R.drawable.vc_weather_rain);
        this.forecastsMapping.put("Snow", R.drawable.vc_weather_snow);
        this.forecastsMapping.put("Atmosphere", R.drawable.vc_weather_atmosphere);
        this.forecastsMapping.put("Clear", R.drawable.vc_weather_clear);
        this.forecastsMapping.put("Extreme", R.drawable.vc_weather_extreme);

        this.isoCountryMapping = new HashMap<>();
        this.isoCountryMapping.put("Brasil", "br");
        this.isoCountryMapping.put("Brazil", "br");
    }

    @Override
    protected String getModuleIdentifier() {
        return WeatherModule.class.getName();
    }

    @Override
    protected TypeToken getStorageTypeToken() {
        return new TypeToken<List<Pair<String, Pair<String, Integer>>>>() {
        };
    }

    @Override
    protected Object getProcessedResult(Object... args) {
        String city = (String) args[0], country = (String) args[1];
        OpenWeatherResponse response;
        try {
            response = endpoint().nextFiveDaysForecast(OPEN_WEATHER_API_KEY,
                    String.format("%s,%s", city, isoCountryMapping.get(country)), UNIT_METRIC).execute().body();
        } catch (IOException e) {
            response = null;
        }
        List<Pair<String, Pair<String, Integer>>> forecastSummary = null;
        if (response != null) {
            // Contamos quantas previsões são de hoje ainda e pulamos todas elas (pra mostrar só a partir de amanhã)
            int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            int todayForecastsCount = 0;
            for (OpenWeatherResponse.Forecast forecast : response.forecasts) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(forecast.dateTime);
                if (calendar.get(Calendar.DAY_OF_MONTH) == today) {
                    todayForecastsCount += 1;
                } else {
                    break;
                }
            }
            if (response.forecasts.size() > 0) {
                forecastSummary = new ArrayList<>();
                // Pega a previsão das 9 AM
                for (int i = todayForecastsCount + 3; i < response.forecasts.size(); i += 7) {
                    OpenWeatherResponse.Forecast forecast = response.forecasts.get(i);
                    Calendar forecastCalendar = Calendar.getInstance();
                    forecastCalendar.setTime(forecast.dateTime);
                    float averageTemperature = (forecast.temperature.minTemp + forecast.temperature.maxTemp) / 2;
                    // TODO: Menos hackish
                    String formattedDate = (String) dateModule.getProcessedResult(forecastCalendar);
                    forecastSummary.add(new Pair<>(formattedDate,
                            // Locale.US pra botar o separador como um ponto
                            new Pair<>(String.format(Locale.US, "%.1f °C", averageTemperature), forecastsMapping.get(forecast.weather.get(0).weather))));
                }
            }
        }
        return forecastSummary;
    }

    private static OpenWeatherAPI endpoint() {
        httpClient.interceptors().clear();

        if (BuildConfig.DEV) {
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient.interceptors().add(logging);
        }

        return retrofit.client(httpClient.build()).build().create(OpenWeatherAPI.class);
    }

    public interface OpenWeatherAPI {

        @GET("forecast")
        Call<OpenWeatherResponse> nextFiveDaysForecast(@Query("APPID") String apiKey, @Query("q") String q, @Query("units") String unit);
    }
}
