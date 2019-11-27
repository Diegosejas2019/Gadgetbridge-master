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
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.util.Date;

public class HistorialActivity extends AppCompatActivity {

    Menu menu;
    String url;
    private static final String TAG = "algo";

    public static final String MY_PREFS_NAME = "FCMSharedPref";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        Bundle b = getIntent().getExtras();
        int value = -1; // or other values
        if(b != null){
            url = b.getString("key");
        }

        clearCache(HistorialActivity.this ,10);

        CargarWebView("http://jmartingimenez-001-site1.itempurl.com/" + url);
    }

    static int clearCacheFolder(final File dir, final int numDays) {

        int deletedFiles = 0;
        if (dir!= null && dir.isDirectory()) {
            try {
                for (File child:dir.listFiles()) {

                    //first delete subdirectories recursively
                    if (child.isDirectory()) {
                        deletedFiles += clearCacheFolder(child, numDays);
                    }

                    //then delete the files and subdirectories in this dir
                    //only empty directories can be deleted, so subdirs have been done first
                    if (child.lastModified() < new Date().getTime() - numDays * DateUtils.DAY_IN_MILLIS) {
                        if (child.delete()) {
                            deletedFiles++;
                        }
                    }
                }
            }
            catch(Exception e) {
                Log.e(TAG, String.format("Failed to clean the cache, error %s", e.getMessage()));
            }
        }
        return deletedFiles;
    }

    /*
     * Delete the files older than numDays days from the application cache
     * 0 means all files.
     */
    public static void clearCache(final Context context, final int numDays) {
        Log.i(TAG, String.format("Starting cache prune, deleting files older than %d days", numDays));
        int numDeletedFiles = clearCacheFolder(context.getCacheDir(), numDays);
        Log.i(TAG, String.format("Cache pruning completed, %d files deleted", numDeletedFiles));
    }
    private void CargarWebView(String url) {
//        final ProgressDialog pd = ProgressDialog.show(WebViewApp.this, "", "Por favor espere, cargando documentos...", true);
        final ProgressDialog pd = new  ProgressDialog(HistorialActivity.this, R.style.GadgetbridgeTheme);
        pd.setMessage("Por favor espere, cargando información..."); // message
        pd.setCancelable(false);
        pd.show();
        final WebView myWebView = (WebView) this.findViewById(R.id.webView2);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setSupportMultipleWindows(true);
        myWebView.getSettings().setLoadWithOverviewMode(true);
        myWebView.getSettings().setUseWideViewPort(true);
        myWebView.getSettings().setBuiltInZoomControls(true);
        myWebView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        myWebView.clearHistory();
        myWebView.clearCache(true);
        myWebView.clearFormData();
        myWebView.setWebChromeClient(new WebChromeClient() {
                                         @Override
                                         public boolean onCreateWindow(WebView view, boolean isDialog,
                                                                       boolean isUserGesture, Message resultMsg) {
                                             WebView newWebView = new WebView(HistorialActivity.this);
                                             newWebView.getSettings().setJavaScriptEnabled(true);
                                             newWebView.getSettings().setSupportZoom(true);
                                             newWebView.getSettings().setBuiltInZoomControls(true);
                                             newWebView.getSettings().setPluginState(WebSettings.PluginState.ON);
                                             newWebView.getSettings().setSupportMultipleWindows(true);
                                             newWebView.getSettings().setAppCacheEnabled(false);
                                             newWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
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
                Toast.makeText(HistorialActivity.this, description, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon)
            {
                pd.show();
                String content = "Registro";
                String[] matches = new String[] {"Registro.aspx", "Login.aspx","RegistroDeUso"};
                Boolean flagDefault = stringContainsItemFromList(url,matches);
                Boolean flag = url.toLowerCase().contains(content.toLowerCase());
                /*if (!flag){
                    //Toast.makeText(LoginViewTest.this, "1", Toast.LENGTH_SHORT).show();
                    PreferenceManager.getDefaultSharedPreferences(HistorialActivity.this).edit().putString("url", url).apply();
                    Toast.makeText(HistorialActivity.this, "Registro exitoso!", Toast.LENGTH_SHORT).show();
                    Intent myIntent = new Intent(HistorialActivity.this, LoginActivity.class);
                    HistorialActivity.this.startActivity(myIntent);
                }
                else{
                    Toast.makeText(HistorialActivity.this, "2", Toast.LENGTH_SHORT).show();
                }*/
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

            SharedPreferences prefs = HistorialActivity.this.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
            String token = prefs.getString("tagtoken", null);
            String postData = "?token=" + token;
            myWebView.loadUrl(url + postData);
        }
        else {
            Toast.makeText(HistorialActivity.this,"Conexión Perdida",Toast.LENGTH_LONG).show();
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
