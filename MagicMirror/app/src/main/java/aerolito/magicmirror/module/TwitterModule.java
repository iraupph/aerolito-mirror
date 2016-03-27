package aerolito.magicmirror.module;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import aerolito.magicmirror.module.base.Module;

public class TwitterModule extends Module {

    private static final String TAG = TwitterModule.class.getName();

    private static final String TRENDSMAP_HOME = "http://trendsmap.com/local/brazil/";
    private static final int TIMEOUT = 15 * 1000;

    private static final String TRENDING_ELEMENT = "a.local-topic-title";

    private static TwitterModule instance = new TwitterModule();

    public static TwitterModule getInstance() {
        return instance;
    }

    private TwitterModule() {
    }

    @Override
    protected String getModuleIdentifier() {
        return TwitterModule.class.getName();
    }

    @Override
    protected Object getProcessedResult(Object... args) {
        List<String> trendingHashtags = null;
        try {
            Document document = Jsoup.connect(TRENDSMAP_HOME).timeout(TIMEOUT).get();
            Elements elementsTrending = document.select(TRENDING_ELEMENT);

            if (elementsTrending.size() > 0) {
                trendingHashtags = new ArrayList<>();
                for (Element e : elementsTrending) {
                    trendingHashtags.add(e.text());
                }
            }
        } catch (IOException e) {
            String message = e.getMessage();
            logger.e(TAG, message, true);
        }
        return trendingHashtags;
    }
}
