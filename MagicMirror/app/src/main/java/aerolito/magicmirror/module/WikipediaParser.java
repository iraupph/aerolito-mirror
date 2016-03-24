package aerolito.magicmirror.module;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WikipediaParser {

    private static final String TAG = WikipediaParser.class.getName();

    public static final String URL_WIKIPEDIA_HOME = "https://pt.wikipedia.org/";

    public static final String EVENTS = "#mf-eventos-atuais ul";
    public static final String TODAY_LISTS = ".plainlinks ul";
    public static final String ITEMS = "li";

    private static WikipediaParser instance = new WikipediaParser();

    public static WikipediaParser getInstance() {
        return instance;
    }

    public void execute(final OnWikipediaProcessedListener onWikipediaProcessedListener) {
        new WikipediaTask(new OnDocumentLoadedListener() {
            private List<String> processItems(Element elements) {
                List<String> items = new ArrayList<>();
                for (Element e : elements.select(ITEMS)) {
                    // Alguns textos referenciam a imagem da seção no site. Tira fora esse texto
                    items.add(e.text().replaceAll("\\(.*imagem\\)", ""));
                }
                return items;
            }

            @Override
            public void onLoaded(Document document) {
                List<String> events = null;
                Elements elementsEvents = document.select(WikipediaParser.EVENTS);
                if (elementsEvents.size() > 0) {
                    events = processItems(elementsEvents.get(0));
                }
                onWikipediaProcessedListener.onLatestEventsProcessed(events);

                List<String> history = null, born = null, deaths = null;
                Elements elementsTodayLists = document.select(WikipediaParser.TODAY_LISTS);
                switch (elementsTodayLists.size()) {
                    case 3:
                        history = processItems(elementsTodayLists.get(0));
                    case 2:
                        born = processItems(elementsTodayLists.get(1));
                    case 1:
                        deaths = processItems(elementsTodayLists.get(2));
                }
                onWikipediaProcessedListener.onTodayHistoryEventsProcessed(history, born, deaths);
            }
        }).execute(URL_WIKIPEDIA_HOME);
    }

    private interface OnDocumentLoadedListener {

        void onLoaded(Document document);
    }

    public interface OnWikipediaProcessedListener {

        void onLatestEventsProcessed(List<String> latestEvents);

        void onTodayHistoryEventsProcessed(List<String> history, List<String> born, List<String> deaths);
    }

    private static class WikipediaTask extends AsyncTask<String, Document, Boolean> {

        private OnDocumentLoadedListener onDocumentLoadedListenerListener;

        private WikipediaTask(OnDocumentLoadedListener onDocumentLoadedListenerListener) {
            this.onDocumentLoadedListenerListener = onDocumentLoadedListenerListener;
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            for (String url : strings) {
                try {
                    Document doc = Jsoup.connect(url).get();
                    publishProgress(doc);
                } catch (IOException e) {
                    Log.e(TAG, "doInBackground: " + e.getMessage());
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Document... values) {
            for (Document document : values) {
                onDocumentLoadedListenerListener.onLoaded(document);
            }
            super.onProgressUpdate(values);
        }
    }
}

