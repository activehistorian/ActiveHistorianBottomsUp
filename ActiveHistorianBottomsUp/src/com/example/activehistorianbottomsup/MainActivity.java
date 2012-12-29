package com.example.activehistorianbottomsup;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.TokenPair;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
	 	private static final String TAG = "ActiveHistorian";
	    final static private String APP_KEY = "le2ytsy64bt80ix";
	    final static private String APP_SECRET = "mtpgb0cdzzp6hj4";
	    final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
	    final static private String ACCOUNT_PREFS_NAME = "prefs";
	    final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
	    final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";
	    static DropboxAPI<AndroidAuthSession> mApi;
	    private boolean mLoggedIn;
	    private Button mSubmit;
	    private LinearLayout mDisplay;
	
	    public static DropboxAPI<?> getMApi(){
			return mApi;
	    }
	    
	    @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);

	        AndroidAuthSession session = buildSession();
	        mApi = new DropboxAPI<AndroidAuthSession>(session);
	        setContentView(R.layout.main);
	        checkAppKeySetup();
	        mSubmit = (Button)findViewById(R.id.linking);

	        mSubmit.setOnClickListener(new OnClickListener() {
	            public void onClick(View v) {
	                  mApi.getSession().startAuthentication(MainActivity.this);
	            }
	        });
	    }

	    @Override
	    protected void onResume() {
	        super.onResume();
	        AndroidAuthSession session = mApi.getSession();
	        
	        if (session.authenticationSuccessful()) {
	            try {
	                session.finishAuthentication();
	                TokenPair tokens = session.getAccessTokenPair();
	                storeKeys(tokens.key, tokens.secret);
	                setLoggedIn(true);
	            } catch (IllegalStateException e) {
	                showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
	                Log.i(TAG, "Error authenticating", e);
	            }
	        }
	    }
	    
	    private void setLoggedIn(boolean loggedIn) {
	    	mLoggedIn = loggedIn;
	    	if (loggedIn) {
	    		Intent intent=new Intent(MainActivity.this,VideoRecording.class);
		        startActivity(intent);
	    	} else {
	    		mSubmit.setText("Link with Dropbox");
	            mDisplay.setVisibility(View.GONE);
	    	}
	    }

	    private void checkAppKeySetup() {
	        Intent testIntent = new Intent(Intent.ACTION_VIEW);
	        String scheme = "db-" + APP_KEY;
	        String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
	        testIntent.setData(Uri.parse(uri));
	        PackageManager pm = getPackageManager();
	        if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
	            showToast("URL scheme in your app's " +
	                    "manifest is not set up correctly. You should have a " +
	                    "com.dropbox.client2.android.AuthActivity with the " +
	                    "scheme: " + scheme);
	            finish();
	        }
	    }

	    private void showToast(String msg) {
	        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
	        error.show();
	    }

	    private String[] getKeys() {
	        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
	        String key = prefs.getString(ACCESS_KEY_NAME, null);
	        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
	        if (key != null && secret != null) {
	        	String[] ret = new String[2];
	        	ret[0] = key;
	        	ret[1] = secret;
	        	return ret;
	        } else {
	        	return null;
	        }
	    }

	    private void storeKeys(String key, String secret) {
	        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
	        Editor edit = prefs.edit();
	        edit.putString(ACCESS_KEY_NAME, key);
	        edit.putString(ACCESS_SECRET_NAME, secret);
	        edit.commit();
	    }

	    private void clearKeys() {
	        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
	        Editor edit = prefs.edit();
	        edit.clear();
	        edit.commit();
	    }

	    private AndroidAuthSession buildSession() {
	        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
	        AndroidAuthSession session;

	        String[] stored = getKeys();
	        if (stored != null) {
	            AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
	            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, accessToken);
	        } else {
	            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
	        }

	        return session;
	    }
	}

