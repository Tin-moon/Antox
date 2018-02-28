package chat.tox.antox.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;

import chat.tox.antox.R;
import chat.tox.antox.theme.ThemeManagerJ;

/**
 * Created by Nechypurenko on 15.02.2018.
 */

public class ColorPickerDialogJ {

    public interface Callback {
        void onColorSelection(int index, int color, int darker);
    }

    private Activity activity;
    private Callback callback;

    private AlertDialog mDialog;

    private int[] colors;

    public ColorPickerDialogJ(Activity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    public void onClickColor(View v) {
        if (v.getTag() != null) {
            Integer index = (Integer) v.getTag();
            callback.onColorSelection(index, colors[index], ThemeManagerJ.darkenColor(colors[index]));
            close();
        }
    }

    private void setBackgroundCompat(View view, Drawable d) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(d);
        } else {
            view.setBackgroundDrawable(d);
        }
    }

    public void show(int preselect) {
        LayoutInflater inflator = activity.getLayoutInflater();
        View view = inflator.inflate(R.layout.dialog_color_chooser, null);

        TypedArray rawColorArray= activity.getResources().obtainTypedArray(R.array.theme_colors);
        colors = new int[rawColorArray.length()];
        for(int i = 0; i < rawColorArray.length(); i++) {
            colors[i] = rawColorArray.getColor(i, 0);
        }
        rawColorArray.recycle();

        GridView list = (GridView) view.findViewById(R.id.color_grid);
        list.setAdapter(new ColorCircleAdapter(activity, colors, preselect));

        mDialog = new AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle)
                .setView(view).setTitle(R.string.dialog_color_picker_title).create();

        if (mDialog.isShowing()) {
            close();
        }
        mDialog.show();
    }

    public boolean isShowing(){
        return mDialog != null && mDialog.isShowing();
    }

    public void close() {
        if(mDialog != null) {
            mDialog.cancel();
        }
    }

    private class ColorCircleAdapter extends BaseAdapter {

        private int[] colors;
        private final LayoutInflater inflater;
        private final int preselect;

        public ColorCircleAdapter(Context context, int[] colors, int preselect) {
            this.colors = colors;
            this.inflater = LayoutInflater.from(context);
            this.preselect = preselect;
        }

        @Override
        public int getCount() {
            return colors.length;
        }

        @Override
        public Integer getItem(int position) {
            return colors[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            FrameLayout child = (FrameLayout) inflater.inflate(R.layout.color_circle, parent, false);
            int color = colors[position];
            child.setTag(position);
            child.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickColor(v);
                }
            });

            child.getChildAt(0).setVisibility(preselect == color ? View.VISIBLE : View.GONE);

            Drawable selector = createSelector(color);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int[][] states = {new int[]{-android.R.attr.state_pressed}, new int[]{android.R.attr.state_pressed}};
                int[] colors = new int[]{ThemeManagerJ.darkenColor(color), color};
                ColorStateList rippleColors = new ColorStateList(states, colors);
                setBackgroundCompat(child, new RippleDrawable(rippleColors, selector, null));
            } else {
                setBackgroundCompat(child, selector);
            }
            return child;
        }

        private Drawable createSelector(int color){
            ShapeDrawable coloredCircle = new ShapeDrawable(new OvalShape());
            coloredCircle.getPaint().setColor(color);

            ShapeDrawable darkerCircle = new ShapeDrawable(new OvalShape());
            darkerCircle.getPaint().setColor(ThemeManagerJ.darkenColor(color));

            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{-android.R.attr.state_pressed}, coloredCircle);
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, darkerCircle);
            return stateListDrawable;
        }
    }

}
