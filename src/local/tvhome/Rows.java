package local.tvhome;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import local.tvhome.core.Row;

/** Inflates one row.xml per Row into a column. */
final class Rows {
    private Rows() {}

    static void fill(LinearLayout column, List<Row> rows, Context ctx) {
        column.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(ctx);
        for (Row row : rows) {
            View v = inf.inflate(R.layout.row, column, false);
            ((TextView) v.findViewById(R.id.label)).setText(row.label);
            ImageView iv = v.findViewById(R.id.icon);
            iv.setImageDrawable(icon(ctx, row));
            if (row.kind == Row.Kind.APP) {
                // App logos are cropped to circles; the line-drawn device and settings icons stay as they are.
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setOval(0, 0, view.getWidth(), view.getHeight());
                    }
                });
                iv.setClipToOutline(true);
            }
            v.setOnClickListener(x -> Launcher.open(ctx, row));
            TextView label = v.findViewById(R.id.label);
            v.setOnFocusChangeListener((x, focused) -> style(x, label, focused));
            style(v, label, false);
            column.addView(v);
        }
    }

    /** Selected row: full brightness, white, medium weight. Everything else fades back to about half. */
    private static void style(View row, TextView label, boolean focused) {
        row.setAlpha(focused ? 1f : 0.55f);
        label.setTextColor(focused ? 0xFFFFFFFF : 0xFFE9E9EE);
        label.setTypeface(Typeface.create(focused ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
    }

    static Drawable icon(Context ctx, Row row) {
        try {
            switch (row.kind) {
                case APP:
                    return ctx.getPackageManager().getApplicationIcon(row.id);
                case INPUT:
                    return ctx.getDrawable("game".equals(row.icon) ? R.drawable.ic_game : R.drawable.ic_box);
                case ACTION:
                    if (row.id.equals("all_apps")) return ctx.getDrawable(R.drawable.ic_apps);
                    if (row.id.equals("tv_settings")) return ctx.getDrawable(R.drawable.ic_tv_settings);
                    return ctx.getDrawable(R.drawable.ic_settings);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
