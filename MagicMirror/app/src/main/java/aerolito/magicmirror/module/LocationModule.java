package aerolito.magicmirror.module;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.util.Pair;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import aerolito.magicmirror.module.base.Module;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;

public class LocationModule extends Module {

    private static final int CITY_ADDRESS_LINE = 1;

    private static final int LOCATION_TIMEOUT = 15 * 1000;

    private String city;
    private String country;

    private Handler locationHandler;
    private FinalizeLocationRunnable locationRunnable;
    private final Object locationSemaphore = new Object();

    private static LocationModule instance = new LocationModule();
    private Context context;

    public static LocationModule getInstance() {
        return instance;
    }

    private LocationModule() {
    }

    @Override
    public void init(Object... args) {
        super.init(args);
        context = (Context) args[0];
        locationHandler = new Handler();
        locationRunnable = new FinalizeLocationRunnable();
    }

    @Override
    protected String getModuleIdentifier() {
        return LocationModule.class.getName();
    }

    @Override
    protected Object getProcessedResult(Object... args) {
        locationHandler.postDelayed(locationRunnable, LOCATION_TIMEOUT);
        SmartLocation.with(context).location()
                .start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        try {
                            List<Address> addresses = new Geocoder(context,
                                    Locale.getDefault()).getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                // Supostamente é o nome da cidade nessa função ;-)
                                String locality = address.getLocality();
                                country = address.getCountryName();
                                if (locality != null) {
                                    city = locality;
                                    finalizeLocation();
                                } else {
                                    city = address.getAddressLine(CITY_ADDRESS_LINE);
                                }
                            }
                        } catch (IOException e) {
                        }
                    }
                });
        synchronized (locationSemaphore) {
            try {
                locationSemaphore.wait();
            } catch (InterruptedException e) {
            }
        }
        return new Pair<>(city, country);
    }

    private void finalizeLocation() {
        SmartLocation.with(context).location().stop();
        synchronized (locationSemaphore) {
            locationSemaphore.notify();
        }
    }

    private class FinalizeLocationRunnable implements Runnable {

        @Override
        public void run() {
            finalizeLocation();
        }
    }
}
