package aerolito.magicmirror.ui.activity;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
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

import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;

import java.util.List;
import java.util.Map;
import java.util.Random;

import aerolito.magicmirror.BuildConfig;
import aerolito.magicmirror.R;
import aerolito.magicmirror.module.DateModule;
import aerolito.magicmirror.module.GreetingModule;
import aerolito.magicmirror.module.LocationModule;
import aerolito.magicmirror.module.TwitterModule;
import aerolito.magicmirror.module.WeatherModule;
import aerolito.magicmirror.module.WikipediaModule;
import aerolito.magicmirror.module.base.Module;
import aerolito.magicmirror.ui.view.RepeatableCountAnimationTextView;
import aerolito.magicmirror.util.L;
import butterknife.Bind;
import butterknife.ButterKnife;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {

    public L logger;

    private static final int HIDE_UI_DELAY = 5000;

    private static final int SLEEP_DELAY = !BuildConfig.DEV ? 30 * 1000 : 60 * 1000 * 15;
    private static final int WAKE_UP_DELAY = 0;

    private static final int OFF_BRIGHTNESS = 0;
    private static final int ON_BRIGHTNESS = !BuildConfig.DEV ? 89 : 255; // Brilho é regulado de 0 até 255 (89 é 35%)

    private static final int MAX_FORECASTS = 3;

    private static final int GREETING_REVEAL_DELAY = 1000;
    private static final int SHIMMER_DURATION = 1500;
    private static final int SHIMMER_START_DELAY = 700;

    private static final String DIGITS_THREAD = "DIGITS_THREAD";
    private static final String HASHTAGS_THREAD = "HASHTAGS_THREAD";

    @Bind(R.id.location) TextView locationView;
    @Bind(R.id.date) TextView dateView;
    @Bind(R.id.forecasts) LinearLayout forecastsView;
    @Bind(R.id.visitors) LinearLayout visitorsView;
    @Bind(R.id.compliment_title) TextView complimentTitleView;
    @Bind(R.id.compliment_content) ShimmerTextView complimentContentView;
    @Bind(R.id.info_title) TextView infoTitleView;
    @Bind(R.id.info_content_text) TextView infoContentText;
    @Bind(R.id.hashtags_title) TextView hashtagsTitleText;
    @Bind(R.id.hashtags) TextView hashtagsText;
    @Bind(R.id.mirror_hashtag) TextView mirrorHashtagText;
    // View que fica por cima do nosso conteúdo quando desliga o espelho
    @Bind(R.id.overlay) View overlayView;

    private Handler uiChangesHandler;

    private Handler wakeUpHandler;
    private Handler sleepHandler;

    private DateModule dateModule = DateModule.getInstance();
    private LocationModule locationModule = LocationModule.getInstance();
    private WeatherModule weatherModule = WeatherModule.getInstance();

    private GreetingModule greetingModule = GreetingModule.getInstance();
    private EventsTask eventsTask;
    private final Shimmer shimmer = new Shimmer()
            .setRepeatCount(ValueAnimator.INFINITE)
            .setDuration(SHIMMER_DURATION)
            .setStartDelay(SHIMMER_START_DELAY)
            .setDirection(Shimmer.ANIMATION_DIRECTION_LTR);
    private final Random aRandom = new Random();
    private final Object digitsLock = new Object();

    private WikipediaModule wikipediaModule = WikipediaModule.getInstance();
    private Animation scrollHorizontallyAnimation;

    private TwitterModule twitterModule = TwitterModule.getInstance();
    private HashtagsTask hashtagsTask;
    //    private Semaphore hashtagsSemaphore;
    //    private Thread twitterResultThread;


    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        logger = L.getInstance(getApplicationContext());
        logger.i("Magic mirror is being started", true);

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

        dateModule.init(logger);
        locationModule.init(logger, getApplicationContext());
        weatherModule.init(logger);
        greetingModule.init(logger);
        wikipediaModule.init(logger);
        twitterModule.init(logger);
    }

    @Override
    protected void onResume() {
        super.onResume();
        makeFullscreen();
        onMirrorActive();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (eventsTask != null) {
            eventsTask.setRunning(false);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
            logger.i("Received Jack input", true);
            onMirrorActive();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onKeyDown(KeyEvent.KEYCODE_HEADSETHOOK, null);
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
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

    private void onMirrorActive() {
        // Ligamos a tela (aumentar o brilho) e agendamos pra apagar (diminuir o brilho) com um delay
        wakeUpNow();
        refreshScheduledSleep();

        dateModule.run(new Module.OnModuleResult() {
            @Override
            public void onModuleResult(Object result) {
                String resultStr = (String) result;
                toggleTextView(dateView, resultStr != null ? View.VISIBLE : View.INVISIBLE, resultStr);
            }
        }, true);
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
                                        for (int i = 0; i < forecasts.size() && i < MAX_FORECASTS; i++) {
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
                            }, false, city, cityAndCountry.second);
                        }
                        toggleTextView(locationView, city != null ? View.VISIBLE : View.INVISIBLE, city);
                    }
                }

        );
        greetingModule.run(new Module.OnModuleResult() {
            private void startCountdown(final RepeatableCountAnimationTextView textView, final int repeats, final char finalValue, final boolean isLastDigit) {
                textView.post(new Runnable() {
                    @Override
                    public void run() {
                        int startValue = aRandom.nextInt(3);
                        int endValue = aRandom.nextInt(5) + 4;
                        final RepeatableCountAnimationTextView repeatableCountAnimationTextView =
                                textView
                                        .setInterpolator(new LinearInterpolator())
                                        .setAnimationDuration(500)
                                        .setRepeatCount(repeats)
                                        .setCountValues(startValue, endValue);
                        repeatableCountAnimationTextView
                                .setCountAnimationListener(new RepeatableCountAnimationTextView.CountAnimationListener() {
                                    @Override
                                    public void onAnimationStart(Object animatedValue) {
                                    }

                                    @Override
                                    public void onAnimationEnd(Object animatedValue) {
                                        if (isLastDigit) {
                                            synchronized (digitsLock) {
                                                digitsLock.notify();
                                            }
                                        }
                                    }

                                    @Override
                                    public void onAnimationRepeat(Object animatedValue, int repeatCount) {
                                        // Começa do final anterior pra não dar flicker na troca de número
                                        // e o final é dependente dele também (decrementa ou incrementa)
                                        // Ou vai até o valor final se é última repetição
                                        int toValue;
                                        int prevEndValue = (int) animatedValue;
                                        if (repeatCount != 0) {
                                            if (prevEndValue > 3) {
                                                toValue = aRandom.nextInt(3);
                                            } else {
                                                toValue = aRandom.nextInt(5) + 4;
                                            }
                                        } else {
                                            // Último é mais rápido pra ficar mais smooth
                                            repeatableCountAnimationTextView.setAnimationDuration(100);
                                            toValue = Integer.valueOf(String.valueOf(finalValue));
                                        }
                                        repeatableCountAnimationTextView.setCountValues(prevEndValue, toValue);
                                    }
                                });
                        repeatableCountAnimationTextView.startCountAnimation();
                    }
                });
            }

            @Override
            public void onModuleResult(Object result) {
                final Pair<String, String> visitorsAndCompliment = (Pair<String, String>) result;
                if (visitorsAndCompliment != null) {
                    for (int i = 0; i < visitorsView.getChildCount(); i++) {
                        visitorsView.getChildAt(i).setVisibility(View.INVISIBLE);
                    }
                    complimentTitleView.setVisibility(View.INVISIBLE);
                    complimentContentView.setVisibility(View.INVISIBLE);
                    final String visitorsStr = visitorsAndCompliment.first;
                    visitorsView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < visitorsView.getChildCount(); i++) {
                                visitorsView.getChildAt(i).setVisibility(View.VISIBLE);
                                int repeats = visitorsView.getChildCount() + i;
                                startCountdown((RepeatableCountAnimationTextView) visitorsView.getChildAt(i), repeats, visitorsStr.charAt(i), i == visitorsView.getChildCount() - 1);
                            }
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    synchronized (digitsLock) {
                                        try {
                                            digitsLock.wait();
                                        } catch (InterruptedException e) {
                                        }
                                    }
                                    complimentTitleView.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            complimentTitleView.setVisibility(View.VISIBLE);
                                            complimentContentView.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    complimentContentView.setText(visitorsAndCompliment.second);
                                                    complimentContentView.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            complimentContentView.setVisibility(View.VISIBLE);
                                                            shimmer.start(complimentContentView);
                                                        }
                                                    });
                                                }
                                            }, GREETING_REVEAL_DELAY);
                                        }
                                    }, GREETING_REVEAL_DELAY);
                                }
                            }, DIGITS_THREAD).start();
                        }
                    }, GREETING_REVEAL_DELAY);
                }
            }
        }, true);
        wikipediaModule.run(new Module.OnModuleResult() {
            @Override
            public void onModuleResult(Object result) {
                List<Map.Entry<String, String>> events = (List<Map.Entry<String, String>>) result;
                if (events != null && events.size() > 0) {
                    if (eventsTask == null || !eventsTask.isRunning()) {
                        eventsTask = new EventsTask(events);
                        eventsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                    eventsTask.setEvents(events);
                }
            }
        });
        twitterModule.run(new Module.OnModuleResult() {
            @Override
            public void onModuleResult(Object result) {
                final List<String> hashtags = (List<String>) result;
                String[] hashtagsArray = hashtags.toArray(new String[hashtags.size()]);
                if (hashtags.size() > 0) {
                    if (hashtagsTask != null) {
                        hashtagsTask.isRunning = false;
                    }
                    hashtagsTask = new HashtagsTask(hashtagsArray);
                    hashtagsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    //                    if (twitterResultThread == null) {
                    //                        twitterResultThread = new AsyncTask<Void, String, Void>(new Runnable() {
                    //                            @Override
                    //                            public void run() {
                    //                                try {
                    //                                    hashtagsSemaphore.acquire();
                    //                                } catch (InterruptedException e) {
                    //                                }
                    //                                mirrorHashtagText.post(new Runnable() {
                    //                                    @Override
                    //                                    public void run() {
                    //                                        hashtagsTitleText.setVisibility(View.VISIBLE);
                    //                                        mirrorHashtagText.setVisibility(View.INVISIBLE);
                    //                                        hashtagsText.setText("");
                    //                                        delayHashtag(hashtags, 0);
                    //                                    }
                    //                                });
                    //                            }
                    //                        }, HASHTAGS_THREAD) {
                    //                            @Override
                    //                            protected Void doInBackground(Void... voids) {
                    //                                return null;
                    //                            }
                    //                        };
                    //                    } else {
                    //                        if (twitterResultThread.isAlive()) {
                    //                            twitterResultThread.interrupt();
                    //                        }
                    //                    }
                    //                    twitterResultThread.start();
                }
            }

        });
    }

    private void toggleTextView(TextView view, int visibility, @Nullable String text) {
        view.setVisibility(visibility);
        if (text != null) {
            view.setText(text);
        }
    }

    private class EventsTask extends AsyncTask<Void, Map.Entry<String, String>, Void> {

        private boolean isRunning;
        private List<Map.Entry<String, String>> events;

        private final Object displayingEventLock = new Object();

        private EventsTask(List<Map.Entry<String, String>> events) {
            this.events = events;
            this.isRunning = true;
        }

        public boolean isRunning() {
            return isRunning;
        }

        public void setRunning(boolean running) {
            isRunning = running;
        }

        public void setEvents(List<Map.Entry<String, String>> events) {
            this.events = events;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            while (isRunning) {
                int newPosition = aRandom.nextInt(events.size());
                for (int i = 0; i < events.size(); i++) {
                    Map.Entry<String, String> entry = events.get(i);
                    if (i == newPosition) {
                        publishProgress(entry);
                        break;
                    }
                }
                synchronized (displayingEventLock) {
                    try {
                        displayingEventLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return null;
        }

        private void animateTicker(final View view, final long durationMillis) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    // Scroll de fora da tela até um pouco mais por trás do título
                    int fullScreenWidth = overlayView.getMeasuredWidth();
                    int contentExtraWidth = fullScreenWidth - view.getMeasuredWidth();
                    scrollHorizontallyAnimation = new TranslateAnimation(fullScreenWidth, (float) (-fullScreenWidth + contentExtraWidth), 0, 0);
                    scrollHorizontallyAnimation.setInterpolator(new LinearInterpolator());
                    scrollHorizontallyAnimation.setDuration(durationMillis);
                    scrollHorizontallyAnimation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            /* Deixa INVISIBLE quando seta o texto pra não dar o flicker da posição original
                            e quando deslocamos pra fora antes de começar a animação, daí aqui já pode mostrar
                            pq ele já tá fora da tela ;-) */
                            view.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            // Isso tira mais um flicker que não sei pq acontece :>
                            view.setVisibility(View.INVISIBLE);
                            synchronized (displayingEventLock) {
                                displayingEventLock.notify();
                            }
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                    view.setAnimation(scrollHorizontallyAnimation);
                }
            });
        }

        @Override
        protected void onProgressUpdate(Map.Entry<String, String>... values) {
            super.onProgressUpdate(values);
            for (Map.Entry<String, String> nextNews : values) {
                toggleTextView(infoTitleView, View.VISIBLE, nextNews.getKey());
                final String content = nextNews.getValue();
                toggleTextView(infoContentText, View.INVISIBLE, content);
                animateTicker(infoContentText, content.length() * 150L);
            }
        }
    }

    private class HashtagsTask extends AsyncTask<Void, String, Void> {

        private static final int HASHTAG_DELAY = 700;

        private String[] hashtags;
        private boolean isRunning;

        private HashtagsTask(String[] hashtags) {
            this.hashtags = hashtags;
            this.isRunning = true;
        }

        public boolean isRunning() {
            return isRunning;
        }

        public void setRunning(boolean running) {
            isRunning = running;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Thread.sleep(HASHTAG_DELAY);
                publishProgress(hashtags);
            } catch (InterruptedException e) {
            }
            return null;
        }

        private void delayHashtag(final String[] hashtags, final int i) {
            final String hashtag = hashtags[i];
            hashtagsText.postDelayed(new Runnable() {
                @Override
                public void run() {
                    String content = hashtagsText.getText().toString();
                    hashtagsText.setText(String.format("%s %s", content, hashtag));
                    if (isRunning && i < hashtags.length - 1 && i < 10) {
                        delayHashtag(hashtags, i + 1);
                    } else {
                        mirrorHashtagText.setVisibility(View.VISIBLE);
                    }
                }
            }, HASHTAG_DELAY);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            hashtagsTitleText.setVisibility(View.VISIBLE);
            mirrorHashtagText.setVisibility(View.INVISIBLE);
            hashtagsText.setText("");
            delayHashtag(values, 0);
        }
    }
}
