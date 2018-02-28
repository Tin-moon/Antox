package chat.tox.antox.activities;

import scala.Option;

/**
 * Created by Nechypurenko on 14.02.2018.
 */

public interface CallReplySelectedListenerJ {
    /**
     * Called when a reply is selected.
     *
     * @param maybeReply the reply string. If this is null, the user intends to use a custom string.
     */
    void onCallReplySelected(Option<String> maybeReply);
}
