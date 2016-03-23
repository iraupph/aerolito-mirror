package aerolito.magicmirror.module;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;

public class WikipediaParser {

    private static final String TAG = WikipediaParser.class.getName();

    public static final String URL_WIKIPEDIA_HOME = "https://pt.wikipedia.org/";

    public static final String EVENTS = "#mf-eventos-atuais ul li";
    public static final String TODAY = ".plainlinks ul li";

    public static void execute(final OnWikipediaProcessedListener onWikipediaProcessedListener) {
        new WikipediaTask(new OnDocumentLoadedListener() {
            @Override
            public void onLoaded(Document document) {
                Elements events = document.select(WikipediaParser.EVENTS);
                for (Element e : events) {
                    Log.i(TAG, e.text());
                }
                onWikipediaProcessedListener.onLatestEventsProcessed(null);
                Elements today = document.select(WikipediaParser.TODAY);
                for (Element e : today) {
                    Log.i(TAG, e.text());
                }
                onWikipediaProcessedListener.onTodayHistoryEventsProcessed(null);
            }
        }).execute(URL_WIKIPEDIA_HOME);
    }

    private interface OnDocumentLoadedListener {

        void onLoaded(Document document);
    }


    public interface OnWikipediaProcessedListener {

        void onLatestEventsProcessed(List<String> latestEvents);

        void onTodayHistoryEventsProcessed(List<String> todayHistoryEvents);
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

