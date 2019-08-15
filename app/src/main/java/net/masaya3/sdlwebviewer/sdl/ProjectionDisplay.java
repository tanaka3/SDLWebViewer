package net.masaya3.sdlwebviewer.sdl;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.smartdevicelink.streaming.video.SdlRemoteDisplay;

import net.masaya3.sdlwebviewer.R;

/**
 * プロジェクション用の画面
 */
public class ProjectionDisplay extends SdlRemoteDisplay {

    public static final String ACTION_VEHICLEDATA = "action_vhicledata";

    /**
     * javascript連動用
     */
    public class JavaScript {


        private Context conxtext;
        private LocalBroadcastManager manager;

        /**
         * コンストラクタ
         * @param context
         */
        public JavaScript(Context context) {
            this.conxtext = context;
            manager = LocalBroadcastManager.getInstance(conxtext);
        }

        /**
         * 車のデータを取得する
         */
        @JavascriptInterface
        public void getVehicleData(){
            Log.d("SDLWebViewer", "JavaScript:getVehicleData");

            final Intent intent = new Intent();
            intent.setAction(SdlService.ACTION_GET_VEHICLEDATA);
            manager.sendBroadcast(intent);
        }
    }


    // ブロードキャストマネージャ
    private LocalBroadcastManager broadcastReceiver;

    //WebView
    private WebView webView;

    // レシーバ
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("SDLWebViewer", "receiver:onReceive");

            if(!intent.getAction().equals(ACTION_VEHICLEDATA)){
                return;
            }

            String json = intent.getStringExtra("vehicle");

            Log.d("SDLWebViewer","reciver:" + json);

            webView.loadUrl(String.format("javascript:getVehicleData('%s')", json));
        }
    };

    public ProjectionDisplay(Context context, Display display) {
        super(context, display);
    }

    /**
     * onStart
     */
    @Override
    protected void onStart() {
        super.onStart();

        //broadcastの登録
        broadcastReceiver = LocalBroadcastManager.getInstance(getContext());

        // レシーバのフィルタをインスタンス化
        final IntentFilter filter = new IntentFilter();
        // フィルタのアクション名を設定する
        filter.addAction(ACTION_VEHICLEDATA);
        // レシーバを登録する
        broadcastReceiver.registerReceiver(receiver, filter);

        // 車情報の定期取得を開始する
        final Intent intent = new Intent();
        intent.setAction(SdlService.ACTION_START_SUBSCRIBE_VEHICLEDATA);
        broadcastReceiver.sendBroadcast(intent);
    }

    /**
     * Stop
     */
    @Override
    protected void onStop() {
        super.onStop();

        // broadcastの解除
        broadcastReceiver.unregisterReceiver(receiver);

        // 車情報の定期取得を停止する
        final Intent intent = new Intent();
        intent.setAction(SdlService.ACTION_STOP_SUBSCRIBE_VEHICLEDATA);
        broadcastReceiver.sendBroadcast(intent);
    }

    @SuppressLint("JavascriptInterface")
    //@SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.projection_layout);

        //アプリケーション用のURLを取得する
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String url = sharedPreferences.getString("sdl_url", getContext().getString(R.string.projection_url));

        webView = (WebView) findViewById(R.id.webView);

        final AppCompatImageView button = (AppCompatImageView)findViewById(R.id.backButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(webView.canGoBack()){
                    webView.goBack();
                }
            }
        });

        //表示設定の場合
        if(!sharedPreferences.getBoolean("use_backkey", true)){
            button.setVisibility(View.GONE);
        }

        webView.setWebViewClient(new WebViewClient(){});

        //URLを指定する
        webView.loadUrl(url);

        //Javascriptを有効にする
        webView.getSettings().setJavaScriptEnabled(true);

        //スクリプト内部での <script src="..."> を動作可能にする
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        //スクリプトからのローカルファイルへのアクセスを許可する
        webView.getSettings().setAllowFileAccessFromFileURLs(true);

        //sdl用Javascriptコールバックの追加
        webView.addJavascriptInterface(new JavaScript(getContext()), "sdl");

    }
}