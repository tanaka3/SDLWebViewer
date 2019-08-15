package net.masaya3.sdlwebviewer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import net.masaya3.sdlwebviewer.sdl.SdlReceiver;
import net.masaya3.sdlwebviewer.sdl.SdlService;
import net.taptappun.taku.kobayashi.runtimepermissionchecker.RuntimePermissionChecker;

/**
 * メイン画面
 */
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.NoActionBar);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //permission
        RuntimePermissionChecker.requestAllPermissions(this, REQUEST_CODE);

        //If we are connected to a module we want to start our SdlService
        if(sharedPreferences.getBoolean("use_wifi", false)) {
            Intent proxyIntent = new Intent(this, SdlService.class);
            startService(proxyIntent);
        }
        else{
            SdlReceiver.queryForConnectedService(this);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setIcon(R.drawable.ic_titile);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.sym_def_app_icon);

        //SDLのService起動
        //アプリケーション用のURLを取得する
        String url = sharedPreferences.getString("main_url", getString(R.string.application_url));
        if(url.isEmpty()){
            url = getString(R.string.application_url);
        }

        //メイン画面のWebView設定
        WebView webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());

        //URLを指定する
        webView.loadUrl(url);

        //Javascriptを有効にする
        webView.getSettings().setJavaScriptEnabled(true);

    }

    /**
     * permissionチェック
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode != REQUEST_CODE)
            return;
        /*
        if(!RuntimePermissionChecker.existConfirmPermissions(this)){
            // write features you want to execute.
        }*/
    }

    /**
     * メニューの作成
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * メニューが選択された場合の処理
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_menu_setting: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_menu_end: {
                Intent intent = new Intent(MainActivity.this, SdlService.class);
                stopService(intent);
                finish();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }
}
