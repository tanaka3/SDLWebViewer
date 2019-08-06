package net.masaya3.sdlwebviewer.sdl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.smartdevicelink.streaming.video.SdlRemoteDisplay;

import net.masaya3.sdlwebviewer.R;

/**
 * プロジェクション用の画面
 */
public class ProjectionDisplay extends SdlRemoteDisplay {

    //javascript連動用
    public class JavaScript {


        private Context conxtext;
        private LocalBroadcastManager manager;


        /**
         * コンストラクタ
         * @param conxtext
         */
        public JavaScript(Context conxtext) {
           manager = LocalBroadcastManager.getInstance(conxtext);
        }

        /**
         * 車のデータを取得する
         * @param isDummy
         */
        public void getVehicleData(boolean isDummy){

        }

        public void startSub(){

        }

        public void stopSub(){

        }
    }


    public ProjectionDisplay(Context context, Display display) {
        super(context, display);
    }

    @SuppressLint("JavascriptInterface")
    //@SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.projection_layout);

        WebView webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());

        //URLを指定する
        webView.loadUrl(getContext().getString(R.string.projection_url));

        //Javascriptを有効にする
        webView.getSettings().setJavaScriptEnabled(true);

        //sdl用Javascriptコールバックの追加
        webView.addJavascriptInterface(new JavaScript(getContext()), "sdl");


        //デバッグ用
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                String msg = String.format("Touch: %d %d", motionEvent.getX(), motionEvent.getY());

                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }
}