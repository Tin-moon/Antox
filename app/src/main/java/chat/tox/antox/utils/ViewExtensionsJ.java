package chat.tox.antox.utils;

import android.view.View;

/**
 * Created by Nechypurenko on 13.02.2018.
 */

public class ViewExtensionsJ {

    public static LocationJ getCenterLocationOnScreen(View view) {
        LocationJ upperLocation = getLocationOnScreen(view);
        return new LocationJ(upperLocation.x() + view.getWidth() / 2, upperLocation.y() + view.getHeight() / 2);
    }

    public static LocationJ getLocationOnScreen(View view){
        int[] rawLocation = new int[2];
        view.getLocationOnScreen(rawLocation);
        return new LocationJ(rawLocation[0], rawLocation[1]);
    }

}
