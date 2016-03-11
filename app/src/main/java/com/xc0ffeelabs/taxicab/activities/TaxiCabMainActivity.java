package com.xc0ffeelabs.taxicab.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.xc0ffeelabs.taxicab.R;
import com.xc0ffeelabs.taxicab.network.AccountManager;
import com.xc0ffeelabs.taxicab.utilities.NetworkUtils;

public class TaxiCabMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taxi_cab_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new CheckOnlineStatus(this).execute();
    }

    private void showLoginActivity() {
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
    }

    private void showNoNetworkMessage() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.no_network_title))
                .setMessage(getString(R.string.no_network_msg))
                .setPositiveButton(getString(R.string.text_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .create()
                .show();
    }

    private class DetermineLoginStatus extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            return TaxiCabApplication.getAccountManager().isCredentialsStored();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                TaxiCabApplication.getAccountManager().autoLoginUser(
                        new AccountManager.LoginStatusCallback() {
                            @Override
                            public void onLoginSuccess() {
                                loggedIn();
                            }

                            @Override
                            public void onLoginFailed() {
                                loginFailed();
                            }
                        }
                );
            } else {
                showLoginActivity();
            }
        }
    }

    private class CheckOnlineStatus extends AsyncTask<Void, Void, Boolean> {

        final private Context mContext;

        public CheckOnlineStatus(Context context) {
            mContext = context;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return NetworkUtils.isNetworkAvailable(mContext) && NetworkUtils.isOnline();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                new DetermineLoginStatus().execute();
            } else {
                showNoNetworkMessage();
            }
        }
    }

    private void loggedIn() {
        // Launch map activity here
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
        finish();
    }

    private void loginFailed() {
        showLoginActivity();
        finish();
    }
 }
