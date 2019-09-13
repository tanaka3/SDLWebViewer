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

    //車両データ送信用ACTION
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

            //service側に取得用メッセージを送信する
            final Intent intent = new Intent();
            intent.setAction(SdlService.ACTION_GET_VEHICLEDATA);
            manager.sendBroadcast(intent);
        }

        /**
         * 車両情報の定期取得を開始する
         */
        @JavascriptInterface
        public void startSubscribeVehicleData(){
            Log.d("SDLWebViewer", "JavaScript:startSubscribeVehicleData");

            //service側に取得用メッセージを送信する
            final Intent intent = new Intent();
            intent.setAction(SdlService.ACTION_START_SUBSCRIBE_VEHICLEDATA);
            manager.sendBroadcast(intent);
        }


        /**
         * 車両情報の定期取得を停止する
         */
        @JavascriptInterface
        public void stopSubscribeVehicleData(){
            Log.d("SDLWebViewer", "JavaScript:stopSubscribeVehicleData");

            //service側に取得用メッセージを送信する
            final Intent intent = new Intent();
            intent.setAction(SdlService.ACTION_STOP_SUBSCRIBE_VEHICLEDATA);
            manager.sendBroadcast(intent);
        }
    }

    // ブロードキャストマネージャ
    private LocalBroadcastManager broadcastReceiver;
    // WebView
    private WebView webView;
    // BroadcastReceiver
    private BroadcastReceiver receiver;

    /**
     * コンストラクア
     * @param context
     * @param display
     */
    public ProjectionDisplay(Context context, Display display) {
        super(context, display);
    }

    /**
     * onStart時
     */
    @Override
    protected void onStart() {
        super.onStart();

        //broadcastの登録
        broadcastReceiver = LocalBroadcastManager.getInstance(getContext());

        //車両情報をHtml側に送信する
        receiver = new BroadcastReceiver() {
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


        // レシーバのフィルタをインスタンス化
        final IntentFilter filter = new IntentFilter();
        // フィルタのアクション名を設定する
        filter.addAction(ACTION_VEHICLEDATA);
        // レシーバを登録する
        broadcastReceiver.registerReceiver(receiver, filter);

        //自動追加の場合は、自動で実行する
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        if(sharedPreferences.getBoolean("use_auto_subscribe", true)) {
            // 車情報の定期取得を開始する
            final Intent intent = new Intent();
            intent.setAction(SdlService.ACTION_START_SUBSCRIBE_VEHICLEDATA);
            broadcastReceiver.sendBroadcast(intent);
        }
    }

    /**
     * Stop時
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.projection_layout);

        //アプリケーション用のURLを取得する
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String url = sharedPreferences.getString("sdl_web_url", getContext().getString(R.string.projection_url));
        if(url.isEmpty()){
            url =  getContext().getString(R.string.projection_url);
        }

        webView = (WebView) findViewById(R.id.webView);

        //戻るボタン
        AppCompatImageView backbutton = (AppCompatImageView)findViewById(R.id.backButton);
        backbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("SDLWebViewer","goBack");
                if(webView.canGoBack()){
                    webView.goBack();
                }
            }
        });

        //表示設定の場合
        if(!sharedPreferences.getBoolean("use_backkey", true)){
            backbutton.setVisibility(View.GONE);
        }

        //ホームボタン
        AppCompatImageView homebutton = (AppCompatImageView)findViewById(R.id.homeButton);
        homebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("SDLWebViewer","goHome");
                String url = sharedPreferences.getString("sdl_web_url", getContext().getString(R.string.projection_url));
                if(url.isEmpty()){
                    url =  getContext().getString(R.string.projection_url);
                }
                //URLを指定する
                webView.loadUrl(url);
            }
        });

        //表示設定の場合
        if(!sharedPreferences.getBoolean("use_homekey", true)){
            homebutton.setVisibility(View.GONE);
        }

        //リロードボタン
        AppCompatImageView reloadbutton = (AppCompatImageView)findViewById(R.id.reloadButton);
        reloadbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("SDLWebViewer","reload");
                webView.reload();
            }
        });

        //表示設定の場合
        if(!sharedPreferences.getBoolean("use_reloadkey", true)){
            reloadbutton.setVisibility(View.GONE);
        }

        //Webviewの設定
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