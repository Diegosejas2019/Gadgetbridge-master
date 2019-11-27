package nodomain.freeyourgadget.gadgetbridge.activities;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

public class ResponsableActivity extends AppCompatActivity {

    private static final String TAG = "algo";
    Menu menu;
    private String currentUrl;
    public static final String MY_PREFS_NAME = "FCMSharedPref";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_responsable);
        clearCache(ResponsableActivity.this ,1);
/*        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.controlcenter_navigation_drawer_open, R.string.controlcenter_navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();*/
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
        myWebView.clearHistory();

        myWebView.clearCache(true);
        myWebView.clearFormData();

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
                                             newWebView.getSettings().setAppCacheEnabled(false);

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
            String postData = "?token=" + token;
            //webview.postUrl(url,postData.getBytes());

            myWebView.loadUrl(url + postData);
        }
        else {
            Toast.makeText(ResponsableActivity.this,"Conexión Perdida",Toast.LENGTH_LONG).show();
        }
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
