package nodomain.freeyourgadget.gadgetbridge.activities;

import androidx.appcompat.app.AppCompatActivity;
import nodomain.freeyourgadget.gadgetbridge.R;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ResponsableActivity extends AppCompatActivity {

    Menu menu;
    private String currentUrl;
    public static final String MY_PREFS_NAME = "FCMSharedPref";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_responsable);
        try {
            CargarWebView("http://jmartingimenez-001-site1.itempurl.com/Panel/Personas");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void CargarWebView(String url) throws UnsupportedEncodingException {
//        final ProgressDialog pd = ProgressDialog.show(WebViewApp.this, "", "Por favor espere, cargando documentos...", true);
        final ProgressDialog pd = new  ProgressDialog(ResponsableActivity.this, R.style.GadgetbridgeTheme);
        pd.setMessage("Por favor espere, cargando documentos..."); // message
        pd.setCancelable(false);
        pd.show();
        final WebView myWebView = (WebView) this.findViewById(R.id.webView);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setSupportMultipleWindows(true);
        myWebView.getSettings().setLoadWithOverviewMode(true);
        myWebView.getSettings().setUseWideViewPort(true);
        myWebView.getSettings().setBuiltInZoomControls(true);
        myWebView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        myWebView.setWebChromeClient(new WebChromeClient() {
                                         @Override
                                         public boolean onCreateWindow(WebView view, boolean isDialog,
                                                                       boolean isUserGesture, Message resultMsg) {
                                             WebView newWebView = new WebView(ResponsableActivity.this);
                                             newWebView.getSettings().setJavaScriptEnabled(true);
                                             newWebView.getSettings().setSupportZoom(true);
                                             newWebView.getSettings().setBuiltInZoomControls(true);
                                             newWebView.getSettings().setPluginState(WebSettings.PluginState.ON);
                                             newWebView.getSettings().setSupportMultipleWindows(true);
                                             view.addView(newWebView);
                                             WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                                             transport.setWebView(newWebView);
                                             resultMsg.sendToTarget();

                                             newWebView.setWebViewClient(new WebViewClient() {
                                                 @Override
                                                 public boolean shouldOverrideUrlLoading(WebView view, String url) {
                                                     Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
                                                     Bundle b = new Bundle();
                                                     b.putBoolean("new_window", true); //sets new window
                                                     intent.putExtras(b);
                                                     startActivity(intent);
                                                     return true;
                                                 }
                                             });
                                             return true;
                                         }
                                     }
        );
        myWebView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(ResponsableActivity.this, description, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon)
            {
                pd.show();
                String content = "Personas";
                String[] matches = new String[] {"Registro.aspx", "Login.aspx","RegistroDeUso"};
                Boolean flagDefault = stringContainsItemFromList(url,matches);
                Boolean flag = url.toLowerCase().contains(content.toLowerCase());
                if (!flag){
                    PreferenceManager.getDefaultSharedPreferences(ResponsableActivity.this).edit().putString("url", url).apply();
                    Toast.makeText(ResponsableActivity.this, "Operación exitosa!", Toast.LENGTH_SHORT).show();
                    Intent myIntent = new Intent(ResponsableActivity.this, LoginActivity.class);
                    ResponsableActivity.this.startActivity(myIntent);
                }
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                pd.dismiss();
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                return false;
            }
        });
        if (isConnectedToInternet()) {

            SharedPreferences prefs = ResponsableActivity.this.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
            String token = prefs.getString("tagtoken", null);
            String postData = "token=" + URLEncoder.encode(token, "UTF-8");
            //webview.postUrl(url,postData.getBytes());

            myWebView.postUrl(url,postData.getBytes());
        }
        else {
            Toast.makeText(ResponsableActivity.this,"Conexión Perdida",Toast.LENGTH_LONG).show();
        }
    }

    public boolean isConnectedToInternet(){
        ConnectivityManager connectivity = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null)
        {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED)
                    {
                        return true;
                    }
        }
        return false;
    }

    public void showOverflowMenu(boolean showMenu){
        if(menu == null)
            return;
        //menu.setGroupVisible(R.id.main_menu_group, showMenu);
    }

    public static boolean stringContainsItemFromList(String inputStr, String[] items)
    {
        for(int i =0; i < items.length; i++)
        {
            if(inputStr.contains(items[i]))
            {
                return true;
            }
        }
        return false;
    }
}
