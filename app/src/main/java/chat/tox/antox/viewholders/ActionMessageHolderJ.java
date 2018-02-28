package chat.tox.antox.viewholders;

import android.view.View;

/**
 * Created by Nechypurenko on 20.02.2018.
 */

public class ActionMessageHolderJ extends GenericMessageHolderJ {

    public ActionMessageHolderJ(View itemView) {
        super(itemView);
    }

    public void setText(String name, String msg) {
        messageText().setText(name + " " + msg);
    }

}
