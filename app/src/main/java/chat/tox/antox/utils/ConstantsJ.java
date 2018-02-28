package chat.tox.antox.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Nechypurenko on 09.02.2018.
 */

public class ConstantsJ {

    public static final String START_TOX = "chat.tox.antox.START_TOX";

    public static final String STOP_TOX = "chat.tox.antox.STOP_TOX";

    public static final String END_CALL = "chat.tox.antox.END_CALL";

    public static final String BROADCAST_ACTION = "chat.tox.antox.BROADCAST";

    public static final String SWITCH_TO_FRIEND = "chat.tox.antox.SWITCH_TO_FRIEND";

    public static final String SWITCH_TO_CALL = "chat.tox.antox.SWITCH_TO_CALL";

    public static final String UPDATE = "chat.tox.antox.UPDATE";

    public static final String START_CALL = "chat.tox.antox.START_CALL";

    public static final String DOWNLOAD_DIRECTORY = "Tox Received Files";

    public static final String AVATAR_DIRECTORY = "avatars";

    public static final String PROFILE_EXPORT_DIRECTORY = "Tox Exported Profiles";

    public static final int ADD_FRIEND_REQUEST_CODE = 0;

    public static final int WELCOME_ACTIVITY_REQUEST_CODE = 3;

    public static final int IMAGE_RESULT = 0;

    public static final int PHOTO_RESULT = 1;

    public static final int FILE_RESULT = 3;

    public static final int UNREAD_COUNT_LIMIT = 99;

    public static final int MAX_NAME_LENGTH = 128;

    public static final int MAX_MESSAGE_LENGTH = 1363; //in bytes

    public static final int MAX_AVATAR_SIZE = 64 * 1024; //in bytes

    public static final List<String> EXIF_FORMATS = new ArrayList<String>(Arrays.asList("jpg", "jpeg"));

}
