package aerolito.magicmirror.module;

import android.animation.ValueAnimator;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.orhanobut.hawk.Hawk;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;

import java.util.Locale;
import java.util.Random;

public class GreetingHelper {

    private static final int VISITOR_DELAY = 1000;
    private static final int SHIMMER_DURATION = 1500;
    private static final int SHIMMER_START_DELAY = 700;

    private String[] COMPLIMENTS = new String[]{
            "RADIANTE", "SEXY", "ESPETACULAR", "SENSUAL", "UM ESTOURO", "ESTONTEANTE", "FENOMENAL",
            "ELEGANTE", "SEM IGUAL", "FODA", "INCRÍVEL", "COM TUDO EM CIMA", "UM ARRASO", "COM TUDO",
            "O MÁXIMO", "EXCELENTE", "AMÁVEL", "PEGÁVEL", "TRANSÁVEL"
    };

    private static GreetingHelper instance = new GreetingHelper();
    private LinearLayout visitorsParent;
    private TextView complimentTitle;
    private ShimmerTextView complimentContent;

    public static GreetingHelper getInstance() {
        return instance;
    }

    public void setup(LinearLayout visitorsParent, TextView complimentTitle, ShimmerTextView complimentContent) {
        this.visitorsParent = visitorsParent;
        this.complimentTitle = complimentTitle;
        this.complimentContent = complimentContent;
    }

    public void updateGreeting() {
        LinearLayout visitorsDigitsParent = (LinearLayout) visitorsParent.getChildAt(1);
        for (int i = 0; i < visitorsDigitsParent.getChildCount(); i++) {
            visitorsDigitsParent.getChildAt(i).setVisibility(View.INVISIBLE);
        }
        complimentTitle.setVisibility(View.INVISIBLE);
        complimentContent.setVisibility(View.INVISIBLE);
        // Incrementa a quantidade de visitas e transforma em String
        int visitors = Hawk.get("visitors", 0) + 1;
        Hawk.put("visitors", visitors);
        String visitorsStr = String.format(Locale.getDefault(), "%04d", visitors);
        setDelayedVisitorDigit(visitorsStr.length() - 1, visitorsStr);
    }

    private void setDelayedVisitorDigit(final int visitorPosition, final String visitorsStr) {
        final TextView visitorDigit = (TextView) ((LinearLayout) visitorsParent.getChildAt(1)).getChildAt(visitorPosition);
        visitorDigit.setVisibility(View.INVISIBLE);
        visitorDigit.setText(String.valueOf(visitorsStr.charAt(visitorPosition)));
        visitorDigit.postDelayed(new Runnable() {
            @Override
            public void run() {
                visitorDigit.setVisibility(View.VISIBLE);
                if (visitorPosition > 0) {
                    setDelayedVisitorDigit(visitorPosition - 1, visitorsStr);
                } else {
                    complimentTitle.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            complimentTitle.setVisibility(View.VISIBLE);
                            complimentContent.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    complimentContent.setVisibility(View.VISIBLE);
                                    complimentContent.setText(COMPLIMENTS[new Random().nextInt(COMPLIMENTS.length)]);
                                    Shimmer shimmer = new Shimmer();
                                    shimmer.setRepeatCount(ValueAnimator.INFINITE)
                                            .setDuration(SHIMMER_DURATION)
                                            .setStartDelay(SHIMMER_START_DELAY)
                                            .setDirection(Shimmer.ANIMATION_DIRECTION_LTR);
                                    shimmer.start(complimentContent);
                                }
                            }, VISITOR_DELAY);
                        }
                    }, VISITOR_DELAY);
                }
            }
        }, VISITOR_DELAY);
    }
}
