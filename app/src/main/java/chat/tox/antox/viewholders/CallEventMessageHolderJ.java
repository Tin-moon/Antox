package chat.tox.antox.viewholders;

import android.view.View;
import android.widget.ImageView;

import chat.tox.antox.R;

/**
 * Created by Nechypurenko on 20.02.2018.
 */

public class CallEventMessageHolderJ extends GenericMessageHolderJ {

    private ImageView prefixedIcon;

    public CallEventMessageHolderJ(View view) {
        super(view);
        prefixedIcon = (ImageView) view.findViewById(R.id.prefixed_icon);
    }

    public void setText(String msg) {
        messageText().setText(msg);
    }

    public void  setPrefixedIcon(int imageRes) {
        prefixedIcon.setImageBitmap(null);
        prefixedIcon.setImageResource(imageRes);
    }

}
