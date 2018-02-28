package chat.tox.antox.utils;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.view.TextureView;
import android.view.View;

import java.util.List;
import java.util.Random;

import chat.tox.antox.wrapper.ToxKey;

/**
 * Created by Nechypurenko on 16.02.2018.
 */

public class UiUtilsJ {

    //Trims an ID so that it can be displayed to the user
    public static String trimId(ToxKey id) {
        final int trimedIdLength = 8;
        return id.toString().substring(0, trimedIdLength - 1);
    }

    public static String sanitizeAddress(String address) {
        //remove start-of-file unicode char and spaces
        return address.replaceAll("\uFEFF", "").replace(" ", "");
    }

    public static String removeNewlines(String str) {
        return str.replace("\n", "").replace("\r", "");
    }

    public static int generateColor(int hash) {
        double goldenRatio = 0.618033988749895D;
        double hue = (new Random(hash).nextFloat() + goldenRatio) % 1;
        return Color.HSVToColor(new float[] { (float)hue * 360, 0.5F, 0.7F });
    }

    public static void toggleViewVisibility(View visibleView, View goneView) {
        visibleView.setVisibility(View.VISIBLE);
        if(goneView != null) {
            goneView.setVisibility(View.GONE);
        }
    }

    public static void toggleViewVisibility(View visibleView, List<View> goneViews) {
        visibleView.setVisibility(View.VISIBLE);
        if(goneViews != null && goneViews.size() > 0) {
            for(View v : goneViews) {
                v.setVisibility(View.GONE);
            }
        }
    }

    public static int getScreenWidth(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    public static int getScreenHeight(Activity activity){
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

    /**
     * Sets the TextureView transform to preserve the aspect ratio of the video.
     */
    public static void adjustAspectRatio(Activity activity, TextureView textureView, int videoWidth, int videoHeight) {
        int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;
        int newWidth;
        int newHeight;
        if (viewHeight > (int)(viewWidth * aspectRatio)) {
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;

        Matrix txform = new Matrix();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textureView.getTransform(txform);
                txform.setScale(((float) newWidth) / viewWidth, ((float) newHeight) / viewHeight);
                txform.postTranslate(xoff, yoff);
                textureView.setTransform(txform);
            }
        });
    }



}
