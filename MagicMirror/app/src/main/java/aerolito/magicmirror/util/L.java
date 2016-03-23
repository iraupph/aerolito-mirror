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
        if (BuildConfig.DEV) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
        Log.i(TAG, msg);
    }

    public void i(String tag, String msg) {
        if (BuildConfig.DEV) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
        Log.i(tag, msg);
    }

    public void e(String msg) {
        if (BuildConfig.DEV) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
        Log.e(TAG, msg);
    }

    public void e(String tag, String msg) {
        if (BuildConfig.DEV) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
        Log.e(tag, msg);
    }
}
