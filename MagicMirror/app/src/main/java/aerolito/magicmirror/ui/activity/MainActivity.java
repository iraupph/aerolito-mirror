package aerolito.magicmirror.ui.activity;

import android.content.ContentResolver;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.yayandroid.locationmanager.LocationBaseActivity;
import com.yayandroid.locationmanager.LocationConfiguration;
import com.yayandroid.locationmanager.LocationManager;
import com.yayandroid.locationmanager.constants.FailType;
import com.yayandroid.locationmanager.constants.LogType;
import com.yayandroid.locationmanager.constants.ProviderType;

import java.io.IOException;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import aerolito.magicmirror.BuildConfig;
import aerolito.magicmirror.R;

public class MainActivity extends LocationBaseActivity {

    private static final String TAG = MainActivity.class.getName();

    private static final int HIDE_UI_DELAY = 1000;

    private static final int SLEEP_DELAY = !BuildConfig.DEV ? 15 * 1000 : 60 * 1000 * 15;
    private static final int WAKE_UP_DELAY = 0;

    private static final int OFF_BRIGHTNESS = 0;
    private static final int ON_BRIGHTNESS = !BuildConfig.DEV ? 89 : 255; // Brilho é regulado de 0 até 255 (89 é 35%)

    private static final String[] WEEKDAYS = new String[]{"", "Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"};

    private SimpleDateFormat dateFormat;

    private Handler uiChangesHandler;

    private Handler wakeUpHandler;
    private Handler sleepHandler;

    private View overlay;
    private TextView location;
    private TextView date;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // View que fica por cima do nosso conteúdo
        overlay = findViewById(R.id.overlay);

        location = (TextView) findViewById(R.id.location);
        date = (TextView) findViewById(R.id.date);

        // Listener pra caso alguém toque na tela esconder as barras do sistema que vão aparecer
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            // Ambas barras estão visíveis, vamos esconder de novo com um delay
                            makeFullscreen();
                        }
                    }
                });

        uiChangesHandler = new Handler();
        wakeUpHandler = new Handler();
        sleepHandler = new Handler();


        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(Locale.getDefault());
        dateFormatSymbols.setWeekdays(WEEKDAYS);
        dateFormat = new SimpleDateFormat("EEEE', 'd ' de 'MMMM", Locale.getDefault());
        dateFormat.setDateFormatSymbols(dateFormatSymbols);
    }

    @Override
    protected void onResume() {
        super.onResume();
        makeFullscreen();
        wakeUpNow();
        refreshScheduledSleep();

        getLocation();
        getDate();
    }

    private void getDate() {
        String date = dateFormat.format(Calendar.getInstance().getTime());
        if (date != null) {
            toggleTextView(this.date, View.VISIBLE, date);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
            // Ligamos a tela (brilho no máximo) e agendamos pra apagar (brilho no mínimo) com um delay
            if (BuildConfig.DEV) {
                Toast.makeText(getApplicationContext(), "RECEIVED JACK INPUT!", Toast.LENGTH_SHORT).show();
            }
            wakeUpNow();
            refreshScheduledSleep();
        }
        return true;
    }

    @Override
    public LocationConfiguration getLocationConfiguration() {
        if (BuildConfig.DEV) {
            LocationManager.setLogType(LogType.GENERAL);
        }

        return new LocationConfiguration()
                .keepTracking(false)
                .useOnlyGPServices(false)
                .askForGooglePlayServices(true)
                .failOnConnectionSuspended(false)
                .doNotUseGooglePlayServices(false)
                .askForEnableGPS(true)
                .setMinAccuracy(200.0f)
                .setWaitPeriod(ProviderType.GOOGLE_PLAY_SERVICES, 5 * 1000)
                .setWaitPeriod(ProviderType.GPS, 15 * 1000)
                .setWaitPeriod(ProviderType.NETWORK, 10 * 1000)
                .setGPSMessage("Por favor habilite o GPS?")
                .setRationalMessage("Permissão necessária.");
    }

    @Override
    public void onLocationFailed(int failType) {
        String message = "Location failed...";
        switch (failType) {
            case FailType.PERMISSION_DENIED: {
                message = "Couldn't get location, because user didn't give permission!";
                break;
            }
            case FailType.GP_SERVICES_NOT_AVAILABLE:
            case FailType.GP_SERVICES_CONNECTION_FAIL: {
                message = "Couldn't get location, because Google Play Services not available!";
                break;
            }
            case FailType.NETWORK_NOT_AVAILABLE: {
                message = "Couldn't get location, because network is not accessible!";
                break;
            }
            case FailType.TIMEOUT: {
                message = "Couldn't get location, and timeout!";
                break;
            }
        }
        Log.e(TAG, "onLocationFailed: " + message);
        toggleTextView(this.location, View.GONE, null);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "onLocationChanged: " + location.toString());
        String locationStr = null;
        try {
            List<Address> addresses = new Geocoder(getApplicationContext(), Locale.getDefault()).getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // Supostamente é o nome da cidade nessa função ;-)
                String locality = address.getLocality();
                if (locality != null) {
                    locationStr = locality;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "onLocationChanged: " + e.getMessage());
        }
        toggleTextView(this.location, locationStr != null ? View.VISIBLE : View.GONE, locationStr);
    }

    private void toggleTextView(TextView view, int visibility, @Nullable String text) {
        view.setVisibility(visibility);
        if (text != null) {
            view.setText(text);
        }
    }

    /**
     * Desliga a tela (diminui o brilho) e esconde o conteúdo após um delay de {@link MainActivity#SLEEP_DELAY}
     */
    private void refreshScheduledSleep() {
        // Remover qualquer agendamento...
        sleepHandler.removeCallbacksAndMessages(null);
        // ... e criar um novo
        sleepHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (BuildConfig.DEV) {
                    Toast.makeText(getApplicationContext(), "SCREEN OFF", Toast.LENGTH_SHORT).show();
                }
                ContentResolver cResolver = getApplicationContext().getContentResolver();
                Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, OFF_BRIGHTNESS);
                overlay.setVisibility(View.VISIBLE);
            }
        }, SLEEP_DELAY);
    }

    /**
     * Liga a tela (aumenta o brilho) e mostra o conteúdo após um delay de {@link MainActivity#WAKE_UP_DELAY}
     */
    private void wakeUpNow() {
        wakeUpHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ContentResolver cResolver = getApplicationContext().getContentResolver();
                Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, ON_BRIGHTNESS);
                overlay.setVisibility(View.GONE);
                if (BuildConfig.DEV) {
                    Toast.makeText(getApplicationContext(), "SCREEN ON", Toast.LENGTH_SHORT).show();
                }
            }
        }, WAKE_UP_DELAY);
    }

    /**
     * Força fullscreen, escondendo a barra de status (topo) e navegação (em baixo) após um pequeno delay
     */
    private void makeFullscreen() {
        uiChangesHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int systemUiFlagHideNavigation = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    systemUiFlagHideNavigation |= View.SYSTEM_UI_FLAG_FULLSCREEN;
                }
                getWindow().getDecorView().setSystemUiVisibility(systemUiFlagHideNavigation);
            }
        }, HIDE_UI_DELAY);
    }
}
