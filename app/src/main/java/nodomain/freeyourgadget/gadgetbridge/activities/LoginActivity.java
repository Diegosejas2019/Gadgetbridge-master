package nodomain.freeyourgadget.gadgetbridge.activities;

import androidx.appcompat.app.AppCompatActivity;
import nodomain.freeyourgadget.gadgetbridge.JSONParser;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.SharedPreference;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP;

public class LoginActivity extends AppCompatActivity {

    JSONParser jParser = new JSONParser();
    private static String url_Servicio = "http://jmartingimenez-001-site1.itempurl.com/api/";
    //private static String url_test = "http://10.0.2.2/api/login/";
    private ProgressDialog pDialog;
    private Integer IDuser;

    private EditText mEmailView;
    private EditText mPasswordView;
    public static final String MY_PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mEmailView = (EditText) findViewById(R.id.correo);
        mPasswordView = (EditText) findViewById(R.id.contrasena);





        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String restoredText = prefs.getString("Email", null);
        if (restoredText != null) {
            String name = prefs.getString("Email", "No name defined");//"No name defined" is the default value.
            String idName = prefs.getString("Password", "None"); //0 is the default value.
            mEmailView.setText(name);
            mPasswordView.setText(idName);
            if (isConnectedToInternet()){
                attemptLogin();
            }
        }

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                    if (isConnectedToInternet()) {
                        attemptLogin();
                    }
                    return true;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnectedToInternet()) {
                    attemptLogin();
                }
            }
        });

/*        TextView mEmailRecoveryPass = (TextView) findViewById(R.id.txtOlvidoContraseña);
        mEmailRecoveryPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnectedToInternet()) {
                    recoverypass();
                }
            }
        });*/


        Button registrarse = (Button) findViewById(R.id.registrarse);
        registrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent myIntent = new Intent(LoginActivity.this, LoginViewTest.class);
                LoginActivity.this.startActivity(myIntent);
            }
        });
    }

    private void recoverypass() {
        mEmailView.setError(null);
        String email = mEmailView.getText().toString();

        boolean cancel = false;
        View focusView = null;

/*        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }*/

        if (cancel) {
            focusView.requestFocus();
        } else {
            new UserRecoveryTask(email).execute();
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
    private void attemptLogin() {
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString("Email", email);
        editor.putString("Password", password);
        editor.apply();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError("Contraseña invalida");
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError("Campo requerido");
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError("Correo invalido");
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {

            //mAuthTask = new UserLoginTask(email, password);
            //mAuthTask.execute((Void) null);
            final String token = SharedPreference.getInstance(LoginActivity.this).getDeviceToken();
            new UserLoginTask(email, password,token).execute();
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }
    private ProgressDialog progressDialog;   // class variable

    private void showProgressDialog(String title, String message)
    {
        progressDialog = new ProgressDialog(this);

        progressDialog.setTitle(title); //title

        progressDialog.setMessage(message); // message

        progressDialog.setCancelable(false);

        progressDialog.show();
    }

    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("Por favor espere...", "Iniciando sesión");
        }

        private final String mEmail;
        private final String mPassword;
        private final String mToken;

        UserLoginTask(String email, String password, String token) {
            mEmail = email;
            mPassword = password;
            mToken = token;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Boolean flag = false;
            /*List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
            nameValuePairs.add(new BasicNameValuePair("email", mEmail));
            nameValuePairs.add(new BasicNameValuePair("contrasenia", mPassword));
            nameValuePairs.add(new BasicNameValuePair("token", "a"));
            nameValuePairs.add(new BasicNameValuePair("nombre", "a"));
            nameValuePairs.add(new BasicNameValuePair("apellido", "b"));*/

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair("usuario", mEmail));
            nameValuePairs.add(new BasicNameValuePair("contrasenia", mPassword));
            nameValuePairs.add(new BasicNameValuePair("token", mToken));

            String Resultado="";
            JSONObject json = jParser.makeHttpRequest(url_Servicio + "Login", "POST", nameValuePairs);

            Log.d("All Products: ", json.toString());
            try {
                if(json != null){
                    String success = json.getString("msg");
                    if (success.equals("ok")){
                        IDuser = json.getInt("id");
                        flag = true;}
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Resultado = e.getMessage();
            }
            return flag;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if(progressDialog != null && progressDialog.isShowing())
            {
                progressDialog.dismiss();
            }

            if (success) {
                Intent myIntent = new Intent(LoginActivity.this, ControlCenterv2.class);
                myIntent.addFlags(FLAG_ACTIVITY_PREVIOUS_IS_TOP);
                myIntent.putExtra("key", IDuser); //Optional parameters
                myIntent.putExtra("email", mEmail); //Optional parameters
                myIntent.putExtra("password", mPassword); //Optional parameters

                SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                editor.putInt("IdUser", IDuser);
                editor.apply();

                LoginActivity.this.startActivity(myIntent);
            } else {
                mPasswordView.setError("Contraseña incorrecta");
                mPasswordView.requestFocus();

            }
        }

        // @Override
        // protected void onCancelled() {
        //   mAuthTask = null;
        // }
    }
    public class UserRecoveryTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        private final String mEmail;

        UserRecoveryTask(String email) {
            mEmail = email;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Boolean flag = false;
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("UserName", mEmail));

            String Resultado="";
            JSONObject json = jParser.makeHttpRequest(url_Servicio + "RecoveryPassword", "POST", nameValuePairs);


            return flag;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if(progressDialog != null && progressDialog.isShowing())
            {
                progressDialog.dismiss();
            }

            if (success) {
                Toast.makeText(LoginActivity.this,"Se ha enviado un mail a la casilla " + mEmail + " para recuperar su contraseña....",Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(LoginActivity.this,"Error al enviar mensaje a correo electronico",Toast.LENGTH_LONG).show();
            }
        }
    }
}
