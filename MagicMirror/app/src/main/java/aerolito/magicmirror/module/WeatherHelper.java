package aerolito.magicmirror.module;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import aerolito.magicmirror.BuildConfig;
import aerolito.magicmirror.R;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class WeatherHelper {

    private static final String TAG = WeatherHelper.class.getName();

    public static final String OPEN_WEATHER_API_KEY = "652e744640b3afa9dfbd94921c24399c";
    public static final String OPEN_WEATHER_API_URL = "http://api.openweathermap.org/data/2.5/";

    public static final String UNIT_METRIC = "metric";

    private static WeatherHelper instance = new WeatherHelper();

    protected static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
    private static HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    private static Retrofit.Builder mRetrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(OPEN_WEATHER_API_URL);


    public static WeatherHelper getInstance() {
        return instance;
    }

    public WeatherHelper() {
        this.forecastsMapping = new HashMap<>();
        this.isoCountryMapping = new HashMap<>();

        this.forecastsMapping.put("Clouds", R.drawable.vc_weather_cloud);
        this.forecastsMapping.put("Thunderstorm", R.drawable.vc_weather_thunderstorm);
        this.forecastsMapping.put("Drizzle", R.drawable.vc_weather_drizzle);
        this.forecastsMapping.put("Rain", R.drawable.vc_weather_rain);
        this.forecastsMapping.put("Snow", R.drawable.vc_weather_snow);
        this.forecastsMapping.put("Atmosphere", R.drawable.vc_weather_atmosphere);
        this.forecastsMapping.put("Clear", R.drawable.vc_weather_clear);
        this.forecastsMapping.put("Extreme", R.drawable.vc_weather_extreme);

        this.isoCountryMapping.put("Brasil", "br");
        this.isoCountryMapping.put("Brazil", "br");
    }

    private HashMap<String, Integer> forecastsMapping;
    private HashMap<String, String> isoCountryMapping;
    private DateHelper dateHelper = DateHelper.getInstance();

    private String formatLocation(String city, String country) {
        return String.format("%s,%s", city, isoCountryMapping.get(country));
    }

    private static OpenWeatherAPI endpoint() {
        httpClient.interceptors().clear();

        if (BuildConfig.DEV) {
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient.interceptors().add(logging);
        }

        return mRetrofit.client(httpClient.build()).build().create(OpenWeatherAPI.class);
    }

    public void requestForecast(final String city, final String country, final OnWeatherListener listener) {
        new OpenWeatherResponseAsyncTask(city, country, listener).execute();
    }

    public interface OnWeatherListener {

        void onWeatherResponse(List<Pair<String, Pair<String, Integer>>> response);
    }

    public interface OpenWeatherAPI {

        @GET("forecast")
        Call<OpenWeatherResponse> nextFiveDaysForecast(@Query("APPID") String apiKey, @Query("q") String q, @Query("units") String unit);
    }

    public class OpenWeatherResponse {

        @SerializedName("list") public List<Forecast> forecasts;

        public class Forecast {

            @SerializedName("dt_txt") public Date dateTime;
            @SerializedName("main") public Main temperature;
            @SerializedName("weather") public List<Weather> weather;
        }

        public class Main {

            @SerializedName("temp_min") public float minTemp;
            @SerializedName("temp_max") public float maxTemp;
        }

        public class Weather {

            @SerializedName("main") public String weather;
        }
    }

    private class OpenWeatherResponseAsyncTask extends AsyncTask<Void, Void, OpenWeatherResponse> {

        private final String city;
        private final String country;
        private final OnWeatherListener listener;

        public OpenWeatherResponseAsyncTask(String city, String country, OnWeatherListener listener) {
            this.city = city;
            this.country = country;
            this.listener = listener;
        }

        @Override
        protected OpenWeatherResponse doInBackground(Void... voids) {
            try {
                return endpoint().nextFiveDaysForecast(OPEN_WEATHER_API_KEY, formatLocation(city, country), UNIT_METRIC).execute().body();
            } catch (IOException e) {
                Log.e(TAG, "requestForecast#doInBackground: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(OpenWeatherResponse response) {
            super.onPostExecute(response);
            if (response != null) {
                List<Pair<String, Pair<String, Integer>>> forecastSummary = new ArrayList<>();
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
                // Pega a previsão das 9 AM
                for (int i = todayForecastsCount + 3; i < response.forecasts.size(); i += 7) {
                    OpenWeatherResponse.Forecast forecast = response.forecasts.get(i);
                    Calendar forecastCalendar = Calendar.getInstance();
                    forecastCalendar.setTime(forecast.dateTime);
                    float averageTemperature = (forecast.temperature.minTemp + forecast.temperature.maxTemp) / 2;
                    forecastSummary.add(new Pair<>(dateHelper.getForecastDate(forecastCalendar),
                            // Locale.US pra botar o separador como um ponto
                            new Pair<>(String.format(Locale.US, "%.1f  °C", averageTemperature), forecastsMapping.get(forecast.weather.get(0).weather))));
                }
                listener.onWeatherResponse(forecastSummary);
            }
        }
    }
}
