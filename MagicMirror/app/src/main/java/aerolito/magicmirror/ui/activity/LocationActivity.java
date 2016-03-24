package aerolito.magicmirror.ui.activity;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;

import com.yayandroid.locationmanager.LocationBaseActivity;
import com.yayandroid.locationmanager.LocationConfiguration;
import com.yayandroid.locationmanager.LocationManager;
import com.yayandroid.locationmanager.constants.FailType;
import com.yayandroid.locationmanager.constants.LogType;
import com.yayandroid.locationmanager.constants.ProviderType;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import aerolito.magicmirror.BuildConfig;
import aerolito.magicmirror.util.L;

public abstract class LocationActivity extends LocationBaseActivity {

    private static final String TAG = LocationActivity.class.getName();

    // Acredito que essa seja a posição do endereço que contém o nome da cidade, supondo que a posição 0 seja o nome da rua
    private static final int CITY_ADDRESS_LINE = 1;

    private static final int[] PROVIDERS = {ProviderType.GOOGLE_PLAY_SERVICES, ProviderType.GPS, ProviderType.NETWORK};
    private static final int PROVIDER_WAIT_PERIOD = 10 * 1000;

    public L log;

    private String city;
    private String country;
    private boolean hasLocality;
    private Handler locationHandler;
    private FinalizeLocationRunnable locationRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = L.getInstance(getApplicationContext());
        locationHandler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hasLocality = false;
        getLocation();
        // Desabilita a busca por localização depois do timeout de todos os providers
        locationRunnable = new FinalizeLocationRunnable();
        locationHandler.postDelayed(locationRunnable, PROVIDER_WAIT_PERIOD * PROVIDERS.length);
    }

    @Override
    public LocationConfiguration getLocationConfiguration() {
        if (BuildConfig.DEV) {
            LocationManager.setLogType(LogType.GENERAL);
        }

        LocationConfiguration locationConfiguration = new LocationConfiguration()
                .keepTracking(true)
                .useOnlyGPServices(false)
                .askForGooglePlayServices(true)
                .failOnConnectionSuspended(false)
                .doNotUseGooglePlayServices(false)
                .askForEnableGPS(true)
                .setGPSMessage("Por favor habilite o GPS")
                .setRationalMessage("Permissão necessária");

        for (int provider : PROVIDERS) {
            locationConfiguration = locationConfiguration.setWaitPeriod(provider, PROVIDER_WAIT_PERIOD);
        }

        return locationConfiguration;

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
        log.e(TAG, "onLocationFailed: " + message);
    }

    @Override
    public void onLocationChanged(Location location) {
        log.i(TAG, "onLocationChanged: " + location.toString());
        if (!hasLocality) {
            try {
                List<Address> addresses = new Geocoder(getApplicationContext(),
                        Locale.getDefault()).getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    // Supostamente é o nome da cidade nessa função ;-)
                    String locality = address.getLocality();
                    country = address.getCountryName();
                    if (locality != null) {
                        city = locality;
                        hasLocality = true;
                        log.i(TAG, "Found locality!");
                        finalizeLocation();
                    } else {
                        city = address.getAddressLine(CITY_ADDRESS_LINE);
                    }
                }
            } catch (IOException e) {
                log.e(TAG, "onLocationChanged " + e.getMessage());
            }
        }
    }

    private void finalizeLocation() {
        log.i(TAG, "Cancelling location tracking after providers wait period");
        getLocationManager().cancel();
        onHasBestLocation(city, country);
    }

    public abstract void onHasBestLocation(String location, String country);

    private class FinalizeLocationRunnable implements Runnable {

        @Override
        public void run() {
            finalizeLocation();
        }
    }
}
