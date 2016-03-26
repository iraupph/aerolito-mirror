package aerolito.magicmirror.application;

import android.app.Application;

import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.HawkBuilder;
import com.orhanobut.hawk.LogLevel;

import aerolito.magicmirror.BuildConfig;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        HawkBuilder hawkBuilder = Hawk.init(this)
                .setEncryptionMethod(HawkBuilder.EncryptionMethod.NO_ENCRYPTION)
                .setStorage(HawkBuilder.newSharedPrefStorage(this));
        if (BuildConfig.DEV) {
            hawkBuilder = hawkBuilder.setLogLevel(LogLevel.FULL);
        }
        hawkBuilder.build();
    }
}
