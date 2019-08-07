package net.masaya3.sdlwebviewer;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * メイン画面
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setIcon(android.R.drawable.sym_def_app_icon);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.sym_def_app_icon);

        //SDLのService起動



        //メイン画面のWebView設定
        WebView webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());

        //URLを指定する
        webView.loadUrl("https://www.yahoo.co.jp/");

        //Javascriptを有効にする
        webView.getSettings().setJavaScriptEnabled(true);

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

        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);

        return true;//super.onOptionsItemSelected(item);
    }
}
