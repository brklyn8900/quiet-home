package local.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import local.tvhome.core.HomeModel;

/** The Settings menu (mode "settings") and the All apps list (mode "apps"). Back closes it. */
public class ListActivity extends Activity {
    static final String MODE = "mode";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean apps = "apps".equals(getIntent().getStringExtra(MODE));
        ((TextView) findViewById(R.id.title)).setText(apps ? "All apps" : "Settings");
        LinearLayout column = findViewById(R.id.list_column);
        Rows.fill(column, apps ? HomeModel.allApps(Launcher.installedApps(this)) : HomeModel.settingsMenu(), this);
        if (column.getChildCount() > 0) column.getChildAt(0).requestFocus();
    }
}
