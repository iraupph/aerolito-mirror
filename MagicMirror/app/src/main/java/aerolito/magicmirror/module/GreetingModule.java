package aerolito.magicmirror.module;

import android.util.Pair;

import com.orhanobut.hawk.Hawk;

import java.util.Random;

import aerolito.magicmirror.module.base.Module;

public class GreetingModule extends Module {

    private String[] COMPLIMENTS = new String[]{
            "Radiante", "Sexy", "Espetacular", "Sensual", "Um Estouro", "Estonteante", "Fenomenal",
            "Elegante", "Sem Igual", "Foda", "Incrível", "Arrasando", "Com Tudo",
            "O Máximo", "Excelente", "Amável", "Pegável", "Transável"
    };

    private static GreetingModule instance = new GreetingModule();

    public static GreetingModule getInstance() {
        return instance;
    }

    private GreetingModule() {
    }

    @Override
    protected String getModuleIdentifier() {
        return GreetingModule.class.getName();
    }

    @Override
    protected Object getProcessedResult(Object... args) {
        int storedVisitorsCountAsInt = Integer.valueOf(Hawk.get(getModuleIdentifier(), new Pair<String, String>("0", null)).first);
        return new Pair<>(String.format(locale, "%04d", storedVisitorsCountAsInt + 1), COMPLIMENTS[new Random().nextInt(COMPLIMENTS.length)]);
    }
}
