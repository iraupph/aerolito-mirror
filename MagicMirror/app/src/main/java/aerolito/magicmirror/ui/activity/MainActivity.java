package aerolito.magicmirror.ui.activity;

import android.content.ContentResolver;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import aerolito.magicmirror.BuildConfig;
import aerolito.magicmirror.R;
import aerolito.magicmirror.module.DateHelper;
import aerolito.magicmirror.module.WeatherHelper;
import aerolito.magicmirror.module.WikipediaParser;
import aerolito.magicmirror.util.L;

public class MainActivity extends LocationActivity implements WeatherHelper.OnWeatherListener {

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

    private WikipediaParser wikipediaHelper = WikipediaParser.getInstance();
    private TextView infoTitle;
    private TextView infoContent;
    private List<Map.Entry<String, String>> news;
    private Animation scrollHorizontallyAnimation;
    private AsyncTask<Void, Map.Entry<String, String>, Void> eventsTask;

    private WeatherHelper weatherHelper = WeatherHelper.getInstance();
    private LinearLayout forecastsParent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        log = L.getInstance(getApplicationContext());

        // View que fica por cima do nosso conteúdo
        overlay = findViewById(R.id.overlay);

        infoTitle = (TextView) findViewById(R.id.info_title);
        infoContent = (TextView) findViewById(R.id.info_content);

        location = (TextView) findViewById(R.id.location);
        date = (TextView) findViewById(R.id.date);

        forecastsParent = (LinearLayout) findViewById(R.id.forecasts);

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        makeFullscreen();
        wakeUpNow();
        refreshScheduledSleep();

        updateDate();

        wikipediaHelper.execute(new OnWikipediaProcessedListener());
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
                overlay.setVisibility(View.INVISIBLE);
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
    public void onHasBestLocation(String city, String country) {
        toggleTextView(this.location, city != null ? View.VISIBLE : View.GONE, city);
        weatherHelper.requestForecast(city, country, this);
    }

    @Override
    public void onWeatherResponse(List<Pair<String, Pair<String, Integer>>> response) {
        for (int i = 0; i < forecastsParent.getChildCount(); i++) {
            ViewGroup forecastView = (ViewGroup) forecastsParent.getChildAt(i);
            TextView dateView = (TextView) forecastView.getChildAt(0);
            ImageView iconView = (ImageView) forecastView.getChildAt(1);
            TextView temperatureView = (TextView) forecastView.getChildAt(2);

            Pair<String, Pair<String, Integer>> forecastData = response.get(i);
            dateView.setText(forecastData.first);
            iconView.setImageResource(forecastData.second.second);
            temperatureView.setText(forecastData.second.first);

            forecastView.setVisibility(View.VISIBLE);
        }
    }

    private class OnWikipediaProcessedListener implements WikipediaParser.OnWikipediaProcessedListener {

        private static final String RECENT_EVENT = "Eventos recentes";
        private static final String HISTORY = "O dia na história";
        private static final String BORN = "Nasceu neste dia";
        private static final String DIED = "Faleceu neste dia";

        private List<Map.Entry<String, String>> temporaryNews;

        private OnWikipediaProcessedListener() {
            this.temporaryNews = new ArrayList<>();
        }

        @Override
        public void onLatestEventsProcessed(List<String> latestEvents) {
            log.i(TAG, "onLatestEventsProcessed");
            for (String event : latestEvents) {
                temporaryNews.add(new AbstractMap.SimpleEntry<>(RECENT_EVENT, event));
            }
        }

        @Override
        public void onTodayHistoryEventsProcessed(List<String> history, List<String> born, List<String> deaths) {
            log.i(TAG, "onTodayHistoryEventsProcessed");
            for (String h : history) {
                temporaryNews.add(new AbstractMap.SimpleEntry<>(HISTORY, h));
            }
            for (String b : born) {
                temporaryNews.add(new AbstractMap.SimpleEntry<>(BORN, b));
            }
            for (String d : deaths) {
                temporaryNews.add(new AbstractMap.SimpleEntry<>(DIED, d));
            }
            // Preenchemos separadamente as notícias nesse cara temporários depois substituimos o principal da classe
            // Essa função é chamada sequencialmente por último então é de boa substituir aqui
            if (temporaryNews.size() > 0) {
                news = temporaryNews;
            }
            if (eventsTask == null) {
                eventsTask = new EventsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    private class EventsTask extends AsyncTask<Void, Map.Entry<String, String>, Void> {

        private boolean isRunning;
        private boolean isDisplayingEvent;

        private EventsTask() {
            this.isRunning = true;
            this.isDisplayingEvent = false;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            log.i(TAG, "Started events display", true);
            while (isRunning) {
                int newPosition = new Random().nextInt(news.size());
                for (int i = 0; i < news.size(); i++) {
                    Map.Entry<String, String> entry = news.get(i);
                    if (i == newPosition && !isDisplayingEvent) {
                        isDisplayingEvent = true;
                        publishProgress(entry);
                        break;
                    }
                }
                while (isDisplayingEvent) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.e(TAG, "EventsTask#doInBackground: " + e.getMessage(), true);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Map.Entry<String, String>... values) {
            super.onProgressUpdate(values);
            for (Map.Entry<String, String> nextNews : values) {
                log.i(TAG, String.format("updateInfo %s - %s", nextNews.getKey(), nextNews.getValue()));
                toggleTextView(infoTitle, View.VISIBLE, nextNews.getKey());
                toggleTextView(infoContent, View.INVISIBLE, nextNews.getValue());
                infoContent.post(new Runnable() {
                    @Override
                    public void run() {
                        // Scroll de fora da tela até um pouco mais por trás do título
                        int fullScreenWidth = overlay.getMeasuredWidth();
                        scrollHorizontallyAnimation = new TranslateAnimation(fullScreenWidth, (float) (-fullScreenWidth * 0.5), 0, 0);
                        scrollHorizontallyAnimation.setInterpolator(new LinearInterpolator());
                        scrollHorizontallyAnimation.setDuration((long) (infoContent.getText().toString().length() * 0.8 * 1000));
                        scrollHorizontallyAnimation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                                /* Deixa INVISIBLE quando seta o texto pra não dar o flicker da posição original
                                e quando deslocamos pra fora antes de começar a animação, daí aqui já pode mostrar
                                pq ele já tá fora da tela ;-) */
                                toggleTextView(infoContent, View.VISIBLE, null);
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                isDisplayingEvent = false;
                                // Isso tira mais um flicker que não sei pq acontece :>
                                toggleTextView(infoContent, View.INVISIBLE, null);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                        });
                        infoContent.setAnimation(scrollHorizontallyAnimation);
                    }
                });
            }
        }
    }
}
