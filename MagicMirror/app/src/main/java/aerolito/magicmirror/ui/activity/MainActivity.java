package aerolito.magicmirror.ui.activity;

import android.content.ContentResolver;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import aerolito.magicmirror.BuildConfig;
import aerolito.magicmirror.R;
import aerolito.magicmirror.module.DateHelper;
import aerolito.magicmirror.module.WikipediaParser;
import aerolito.magicmirror.util.L;

public class MainActivity extends LocationActivity {

    private static final String TAG = MainActivity.class.getName();

    public L log;

    private static final int HIDE_UI_DELAY = 1000;

    private static final int SLEEP_DELAY = !BuildConfig.DEV ? 15 * 1000 : 60 * 1000 * 15;
    private static final int WAKE_UP_DELAY = 0;

    private static final int OFF_BRIGHTNESS = 0;
    private static final int ON_BRIGHTNESS = !BuildConfig.DEV ? 89 : 255; // Brilho é regulado de 0 até 255 (89 é 35%)

    private Handler uiChangesHandler;

    private Handler wakeUpHandler;
    private Handler sleepHandler;

    private View overlay;
    private TextView location;

    private DateHelper dateHelper = DateHelper.getInstance();
    private TextView date;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        log = L.getInstance(getApplicationContext());

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

        WikipediaParser.execute(new WikipediaParser.OnWikipediaProcessedListener() {
            @Override
            public void onLatestEventsProcessed(List<String> latestEvents) {
                log.i(TAG, "onLatestEventsProcessed: ");
            }

            @Override
            public void onTodayHistoryEventsProcessed(List<String> todayHistoryEvents) {
                log.i(TAG, "onTodayHistoryEventsProcessed: ");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        makeFullscreen();
        wakeUpNow();
        refreshScheduledSleep();

        updateDate();
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

    private void toggleTextView(TextView view, int visibility, @Nullable String text) {
        view.setVisibility(visibility);
        if (text != null) {
            view.setText(text);
        }
    }

    private void updateDate() {
        String date = dateHelper.getDate();
        toggleTextView(this.date, date != null ? View.VISIBLE : View.GONE, date);
    }

    @Override
    public void onHasBestLocation(String location) {
        toggleTextView(this.location, location != null ? View.VISIBLE : View.GONE, location);
    }
}
