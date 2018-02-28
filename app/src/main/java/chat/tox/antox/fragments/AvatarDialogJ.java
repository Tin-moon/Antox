package chat.tox.antox.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

import chat.tox.antox.R;
import chat.tox.antox.data.AntoxDB;
import chat.tox.antox.data.State;
import chat.tox.antox.utils.BitmapManager;
import chat.tox.antox.utils.ConstantsJ;
import chat.tox.antox.utils.FileUtils;
import chat.tox.antox.wrapper.FileKind;
import chat.tox.antox.wrapper.FileKind$;
import scala.Option;

/**
 * Created by Nechypurenko on 15.02.2018.
 */

public class AvatarDialogJ {

    private final Activity activity;
    private final SharedPreferences preferences;
    private Dialog mDialog;

    public AvatarDialogJ(Activity activity) {
        this.activity = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        this.mDialog = null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String name = preferences.getString("tox_id", "");
            File avatarFile = new File(FileKind.AVATAR$.MODULE$.getStorageDir(activity), name);
            if (requestCode == ConstantsJ.IMAGE_RESULT) {
                Uri uri = data.getData();
                String[] filePathColumn = new String[]{MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
                CursorLoader loader = new CursorLoader(activity, uri, filePathColumn, null, null, null);
                Cursor cursor = loader.loadInBackground();
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        File imageFile = new File(cursor.getString(cursor.getColumnIndexOrThrow(filePathColumn[0])));
                        if (!imageFile.exists()) {
                            return;
                        }
                        FileUtils.copy(imageFile, avatarFile);
                    }
                }
            }

            Bitmap bitmap = resizeAvatar(avatarFile);
            if (bitmap != null) {
                FileUtils.writeBitmap(bitmap, Bitmap.CompressFormat.PNG, 0, avatarFile);
                BitmapManager.setAvatarInvalid(avatarFile);
                State.userDb(activity).updateActiveUserDetail("avatar", name);
            } else {
                Toast.makeText(activity, activity.getString(R.string.avatar_too_large_error), Toast.LENGTH_SHORT).show();
            }
            AntoxDB db = State.db();
            db.setAllFriendReceivedAvatar(false);
            State.transfers().updateSelfAvatar(activity, true);
        }
    }

    private Bitmap resizeAvatar(File avatar) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = BitmapManager.calculateInSampleSize(options, 256);
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inJustDecodeBounds = false;
        Bitmap rawBitmap = BitmapFactory.decodeFile(avatar.getPath(), options);

        int cropDimension = 0;
        if (rawBitmap.getWidth() >= rawBitmap.getHeight()) {
            cropDimension = rawBitmap.getHeight();
        } else {
            cropDimension = rawBitmap.getWidth();
        }

        Bitmap bitmap = ThumbnailUtils.extractThumbnail(rawBitmap, cropDimension, cropDimension);
        int MAX_DIMENSIONS = 256;
        int MIN_DIMENSIONS = 16;

        int currSize = MAX_DIMENSIONS;
        while (currSize >= MIN_DIMENSIONS && getSizeInBytes(bitmap) > ConstantsJ.MAX_AVATAR_SIZE) {
            bitmap = Bitmap.createScaledBitmap(bitmap, currSize, currSize, false);
            currSize /= 2;
        }

        if (getSizeInBytes(bitmap) > ConstantsJ.MAX_AVATAR_SIZE) {
            return null;
        } else {
            return bitmap;
        }
    }

    private long getSizeInBytes(Bitmap bitmap) {
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    public void refreshAvatar(ImageView avatarView) {
        Option<File> avatar = FileKind.AVATAR$.MODULE$.getAvatarFile(preferences.getString("avatar", ""), activity);
        if (avatar.isDefined() && avatar.get().exists()) {
            avatarView.setImageURI(Uri.fromFile(avatar.get()));
        } else {
            avatarView.setImageResource(R.drawable.ic_action_contact);
        }
    }

    public void show() {
        LayoutInflater inflator = activity.getLayoutInflater();
        View view = inflator.inflate(R.layout.dialog_avatar, null);
        mDialog = new AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle).setView(view).create();

        Button photoButton = (Button) view.findViewById(R.id.avatar_takephoto);
        Button fileButton = (Button) view.findViewById(R.id.avatar_pickfile);

        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                if (cameraIntent.resolveActivity(activity.getPackageManager()) != null) {
                    String fileName = preferences.getString("tox_id", "");
                    try {
                        File file = new File(FileKind.AVATAR$.MODULE$.getStorageDir(activity), fileName);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                        activity.startActivityForResult(cameraIntent, ConstantsJ.PHOTO_RESULT);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(activity, activity.getString(R.string.no_camera_intent_error), Toast.LENGTH_SHORT).show();
                }
            }
        });

        fileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activity.startActivityForResult(intent, ConstantsJ.IMAGE_RESULT);
            }
        });

        refreshAvatar((ImageView) view.findViewById(R.id.avatar_image));
        if (isShowing()) {
            close();
        }
        mDialog.show();
    }

    public boolean isShowing() {
        return mDialog != null && mDialog.isShowing();
    }

    public void close() {
        if (mDialog != null) {
            mDialog.cancel();
        }
    }

}
