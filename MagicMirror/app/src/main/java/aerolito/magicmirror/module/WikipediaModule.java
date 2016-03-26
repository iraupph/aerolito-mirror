package aerolito.magicmirror.module;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import aerolito.magicmirror.module.base.Module;

public class WikipediaModule extends Module {

    private final String WIKIPEDIA_BR_HOME = "https://pt.wikipedia.org/";

    private final String RECENT_ELEMENT = "#mf-eventos-atuais ul";
    private final String HISTORY_ELEMENT = ".plainlinks ul";
    private final String ITEM_ELEMENT = "li";

    private final String RECENT_TITLE = "Eventos recentes";
    private final String HISTORY_TITLE = "O dia na história";
    private final String BORN_TITLE = "Nasceu neste dia";
    private final String DIED_TITLE = "Faleceu neste dia";

    private static WikipediaModule instance = new WikipediaModule();

    public static WikipediaModule getInstance() {
        return instance;
    }

    private WikipediaModule() {
    }

    @Override
    protected String getModuleIdentifier() {
        return WikipediaModule.class.getName();
    }

    @Override
    protected Object getProcessedResult(Object... args) {
        List<Map.Entry<String, String>> titleAndEventList = null;
        try {
            Document document = Jsoup.connect(WIKIPEDIA_BR_HOME).get();
            Elements elementsEvents = document.select(RECENT_ELEMENT);
            Elements elementsTodayLists = document.select(HISTORY_ELEMENT);

            if (elementsEvents.size() > 0 || elementsTodayLists.size() > 0) {
                titleAndEventList = new ArrayList<>();

                if (elementsEvents.size() > 0) {
                    titleAndEventList.addAll(processItems(RECENT_TITLE, elementsEvents.get(0)));
                }

                switch (elementsTodayLists.size()) {
                    case 3:
                        titleAndEventList.addAll(processItems(HISTORY_TITLE, elementsTodayLists.get(0)));
                    case 2:
                        titleAndEventList.addAll(processItems(BORN_TITLE, elementsTodayLists.get(1)));
                    case 1:
                        titleAndEventList.addAll(processItems(DIED_TITLE, elementsTodayLists.get(2)));
                }
            }
        } catch (IOException e) {
        }
        return titleAndEventList;
    }

    private List<Map.Entry<String, String>> processItems(String title, Element elements) {
        List<Map.Entry<String, String>> items = new ArrayList<>();
        for (Element e : elements.select(ITEM_ELEMENT)) {
            // Alguns textos referenciam a imagem da seção no site. Tira fora esse texto
            items.add(new AbstractMap.SimpleEntry<>(title, e.text().replaceAll("\\(.*imagem\\)", "")));
        }
        return items;
    }
}
