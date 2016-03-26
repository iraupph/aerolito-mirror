package aerolito.magicmirror.ui.activity;

import android.animation.ValueAnimator;
import android.content.ContentResolver;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
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

import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.HawkBuilder;
import com.orhanobut.hawk.LogLevel;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import aerolito.magicmirror.BuildConfig;
import aerolito.magicmirror.R;
import aerolito.magicmirror.module.DateModule;
import aerolito.magicmirror.module.GreetingModule;
import aerolito.magicmirror.module.LocationModule;
import aerolito.magicmirror.module.WeatherModule;
import aerolito.magicmirror.module.WikipediaHelper;
import aerolito.magicmirror.module.base.Module;
import aerolito.magicmirror.util.L;
import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    public L log;

    private static final int HIDE_UI_DELAY = 1000;

    private static final int SLEEP_DELAY = !BuildConfig.DEV ? 15 * 1000 : 60 * 1000 * 15;
    private static final int WAKE_UP_DELAY = 0;

    private static final int OFF_BRIGHTNESS = 0;
    private static final int ON_BRIGHTNESS = !BuildConfig.DEV ? 89 : 255; // Brilho é regulado de 0 até 255 (89 é 35%)

    private static final int GREETING_REVEAL_DELAY = 1000;
    private static final int SHIMMER_DURATION = 1500;
    private static final int SHIMMER_START_DELAY = 700;

    @Bind(R.id.location) TextView locationView;
    @Bind(R.id.date) TextView dateView;
    @Bind(R.id.forecasts) LinearLayout forecastsView;
    @Bind(R.id.compliment_title) TextView complimentTitleView;
    @Bind(R.id.compliment_content) ShimmerTextView complimentContentView;
    @Bind(R.id.info_title) TextView infoTitleView;
    @Bind(R.id.info_content) TextView infoContentView;
    // View que fica por cima do nosso conteúdo quando desliga o espelho
    @Bind(R.id.overlay) View overlayView;
    @Bind(R.id.visitors) LinearLayout visitorsView;

    private Handler uiChangesHandler;

    private Handler wakeUpHandler;
    private Handler sleepHandler;

    private DateModule dateModule = DateModule.getInstance();
    private LocationModule locationModule = LocationModule.getInstance();
    private WeatherModule weatherModule = WeatherModule.getInstance();
    private GreetingModule greetingModule = GreetingModule.getInstance();

    private WikipediaHelper wikipediaHelper = WikipediaHelper.getInstance();
    private List<Map.Entry<String, String>> news;
    private Animation scrollHorizontallyAnimation;
    private AsyncTask<Void, Map.Entry<String, String>, Void> eventsTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        log = L.getInstance(getApplicationContext());

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

        HawkBuilder hawkBuilder = Hawk.init(this)
                .setEncryptionMethod(HawkBuilder.EncryptionMethod.NO_ENCRYPTION)
                .setStorage(HawkBuilder.newSharedPrefStorage(this));
        if (BuildConfig.DEV) {
            hawkBuilder = hawkBuilder.setLogLevel(LogLevel.FULL);
        }
        hawkBuilder.build();

        dateModule.init();
        locationModule.init(getApplicationContext());
        weatherModule.init();
        greetingModule.init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        makeFullscreen();
        wakeUpNow();
        refreshScheduledSleep();
        dateModule.run(new Module.OnModuleResult() {
            @Override
            public void onModuleResult(Object result) {
                String resultStr = (String) result;
                toggleTextView(dateView, resultStr != null ? View.VISIBLE : View.INVISIBLE, resultStr);
            }
        });
        locationModule.run(
                new Module.OnModuleResult() {
                    @Override
                    public void onModuleResult(Object result) {
                        Pair<String, String> cityAndCountry = (Pair<String, String>) result;
                        String city = null;
                        if (cityAndCountry != null) {
                            city = cityAndCountry.first;
                            weatherModule.run(new Module.OnModuleResult() {
                                @Override
                                public void onModuleResult(Object result) {
                                    List<Pair<String, Pair<String, Integer>>> forecasts = (List<Pair<String, Pair<String, Integer>>>) result;
                                    if (forecasts != null) {
                                        forecastsView.setVisibility(View.VISIBLE);
                                        for (int i = 0; i < forecastsView.getChildCount(); i++) {
                                            ViewGroup forecastView = (ViewGroup) forecastsView.getChildAt(i);
                                            TextView dateView = (TextView) forecastView.getChildAt(0);
                                            ImageView iconView = (ImageView) forecastView.getChildAt(1);
                                            TextView temperatureView = (TextView) forecastView.getChildAt(2);

                                            Pair<String, Pair<String, Integer>> forecastData = forecasts.get(i);
                                            dateView.setText(forecastData.first);
                                            iconView.setImageResource(forecastData.second.second);
                                            temperatureView.setText(forecastData.second.first);

                                            forecastView.setVisibility(View.VISIBLE);
                                        }
                                    } else {
                                        forecastsView.setVisibility(View.INVISIBLE);
                                    }
                                }
                            }, city, cityAndCountry.second);
                        }

                        toggleTextView(locationView, city != null ? View.VISIBLE : View.INVISIBLE, city);
                    }
                }

        );
        wikipediaHelper.execute(new

                OnWikipediaProcessedListener()

        );
        greetingModule.run(new Module.OnModuleResult() {
            private void setDelayedVisitorDigit(final int visitorPosition, final String visitorsStr, final String compliment) {
                final TextView visitorDigit = (TextView) visitorsView.getChildAt(visitorPosition);
                visitorDigit.setText(String.valueOf(visitorsStr.charAt(visitorPosition)));
                visitorDigit.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        visitorDigit.setVisibility(View.VISIBLE);
                        if (visitorPosition > 0) {
                            setDelayedVisitorDigit(visitorPosition - 1, visitorsStr, compliment);
                        } else {
                            complimentTitleView.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    complimentTitleView.setVisibility(View.VISIBLE);
                                    complimentContentView.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            complimentContentView.setVisibility(View.VISIBLE);
                                            complimentContentView.setText(compliment);
                                            Shimmer shimmer = new Shimmer();
                                            shimmer.setRepeatCount(ValueAnimator.INFINITE)
                                                    .setDuration(SHIMMER_DURATION)
                                                    .setStartDelay(SHIMMER_START_DELAY)
                                                    .setDirection(Shimmer.ANIMATION_DIRECTION_LTR);
                                            shimmer.start(complimentContentView);
                                        }
                                    }, GREETING_REVEAL_DELAY);
                                }
                            }, GREETING_REVEAL_DELAY);
                        }
                    }
                }, GREETING_REVEAL_DELAY);
            }

            @Override
            public void onModuleResult(Object result) {
                Pair<String, String> visitorsAndCompliment = (Pair<String, String>) result;
                if (visitorsAndCompliment != null) {
                    for (int i = 0; i < visitorsView.getChildCount(); i++) {
                        visitorsView.getChildAt(i).setVisibility(View.INVISIBLE);
                    }
                    complimentTitleView.setVisibility(View.INVISIBLE);
                    complimentContentView.setVisibility(View.INVISIBLE);
                    String visitorsStr = visitorsAndCompliment.first;
                    setDelayedVisitorDigit(visitorsStr.length() - 1, visitorsStr, visitorsAndCompliment.second);
                }
            }
        });
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
                overlayView.setVisibility(View.VISIBLE);
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
                overlayView.setVisibility(View.INVISIBLE);
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

    private class OnWikipediaProcessedListener implements WikipediaHelper.OnWikipediaProcessedListener {

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
                toggleTextView(infoTitleView, View.VISIBLE, nextNews.getKey());
                toggleTextView(infoContentView, View.INVISIBLE, nextNews.getValue() + "\n");
                infoContentView.post(new Runnable() {
                    @Override
                    public void run() {
                        // Scroll de fora da tela até um pouco mais por trás do título
                        int fullScreenWidth = overlayView.getMeasuredWidth();
                        int contentExtraWidth = fullScreenWidth - infoContentView.getMeasuredWidth();
                        scrollHorizontallyAnimation = new TranslateAnimation(fullScreenWidth, (float) (-fullScreenWidth + contentExtraWidth), 0, 0);
                        scrollHorizontallyAnimation.setInterpolator(new LinearInterpolator());
                        scrollHorizontallyAnimation.setDuration(infoContentView.getText().toString().length() * 1000L);
                        scrollHorizontallyAnimation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                                /* Deixa INVISIBLE quando seta o texto pra não dar o flicker da posição original
                                e quando deslocamos pra fora antes de começar a animação, daí aqui já pode mostrar
                                pq ele já tá fora da tela ;-) */
                                toggleTextView(infoContentView, View.VISIBLE, null);
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                isDisplayingEvent = false;
                                // Isso tira mais um flicker que não sei pq acontece :>
                                toggleTextView(infoContentView, View.INVISIBLE, null);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                        });
                        infoContentView.setAnimation(scrollHorizontallyAnimation);
                    }
                });
            }
        }
    }
}
