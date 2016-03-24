package aerolito.magicmirror.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import aerolito.magicmirror.BuildConfig;

public class L {

    private static L instance = new L();
    private Context context;

    public static L getInstance(Context context) {
        instance.context = context;
        return instance;
    }

    private L() {
    }

    private final String TAG = L.class.getName();

    public void i(String msg) {
        i(msg, false);
    }

    public void i(String msg, boolean disableToast) {
        i(TAG, msg, disableToast);
    }

    public void i(String tag, String msg) {
        i(tag, msg, false);
    }


    public void i(String tag, String msg, boolean disableToast) {
        if (BuildConfig.DEV && !disableToast) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
        Log.i(tag, msg);
    }

    public void e(String msg) {
        e(msg, false);
    }

    public void e(String msg, boolean disableToast) {
        e(TAG, msg, disableToast);
    }

    public void e(String tag, String msg) {
        e(tag, msg, false);
    }

    public void e(String tag, String msg, boolean disableToast) {
        if (BuildConfig.DEV && !disableToast) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
        Log.e(tag, msg);
    }
}
