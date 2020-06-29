package com.example.letsconnect;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final String EMAIL = "email";
    CallbackManager callbackManager = CallbackManager.Factory.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.example.letsconnect",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.getMessage();
        } catch (NoSuchAlgorithmException e) {
            e.getMessage();
        }
        AppEventsLogger.activateApp(getApplication());
        final LoginButton facebookLogin = findViewById(R.id.facebook_login);
        facebookLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facebookLogin.setPermissions(Arrays.asList(EMAIL));

                facebookLogin.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Toast.makeText(MainActivity.this, "Sucess", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(MainActivity.this, "Login Cancelled", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Toast.makeText(MainActivity.this, "Login Failed", Toast.LENGTH_LONG).show();
                        Log.e("Main Activity", "" + exception.getMessage());
                    }
                });

            }
        });
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        ConstraintLayout constraintLayout = findViewById(R.id.parent_layout);
        if (!isLoggedIn) {
            Snackbar.make(constraintLayout, "You have been logged out so log in", Snackbar.LENGTH_SHORT).show();

        }
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile"));
        if (accessToken != null) {
            getUserName(accessToken);
        } else {
            TextView username = findViewById(R.id.userName);
            username.setText(R.string.default_text);
        }

    }

    private void getUserName(AccessToken accessToken) {
        GraphRequest request = GraphRequest.newMeRequest(accessToken, new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                TextView username = findViewById(R.id.userName);
                String name = null;
                try {
                    if (object.has("first_name")) {
                        name = object.getString("first_name");

                    }
                    if (object.has("last_name")) {
                        name += " " + object.getString("last_name");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (name.isEmpty()) {
                    username.setText("sorry no name");
                }
                username.setText(name);
            }
        });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "first_name,last_name");
        request.setParameters(parameters);
        request.executeAsync();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}