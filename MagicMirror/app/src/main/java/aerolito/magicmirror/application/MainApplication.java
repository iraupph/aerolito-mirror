package aerolito.magicmirror.application;

import android.app.Application;

import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.HawkBuilder;
import com.orhanobut.hawk.LogLevel;

import aerolito.magicmirror.BuildConfig;
import aerolito.magicmirror.R;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

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

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("font/Arvil_Sans.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );
    }
}
