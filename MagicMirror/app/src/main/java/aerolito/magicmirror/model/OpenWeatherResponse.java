package aerolito.magicmirror.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

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
