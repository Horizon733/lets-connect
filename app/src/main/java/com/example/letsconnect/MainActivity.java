package com.example.letsconnect;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.snapchat.kit.sdk.login.models.UserDataResponse;
import com.snapchat.kit.sdk.login.networking.FetchUserDataCallback;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import com.snapchat.kit.sdk.SnapLogin;
import com.snapchat.kit.sdk.core.controller.LoginStateController;
import com.snapchat.kit.sdk.login.models.MeData;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.facebook_login)
    LoginButton facebookLogin;
    @BindView(R.id.userName)
    TextView username;
    @BindView(R.id.twitter_login)
    TwitterLoginButton twitterLogin;
    @BindView(R.id.twitter_user_id)
    TextView twitterUserId;
    @BindView(R.id.snapchat_login)
    Button snapchat_login_button;
    @BindView(R.id.snapchat_user_id)
    TextView snapchat_name;
    private static final String EMAIL = "email";
    CallbackManager callbackManager = CallbackManager.Factory.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TwitterConfig config = new TwitterConfig.Builder(this)
                .logger(new DefaultLogger(Log.DEBUG))
                .twitterAuthConfig(new TwitterAuthConfig(getResources().getString(R.string.twitter_api_key), getResources().getString(R.string.twitter_api_secret_key)))
                .debug(true)
                .build();
        Twitter.initialize(config);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
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
        facebookLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facebookClick();
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
            username.setText(R.string.default_text);
        }
        twitterUserId.setText(R.string.twitter_default_text);
        twitterLogin.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                Toast.makeText(MainActivity.this, "Sucess", Toast.LENGTH_SHORT).show();
                TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();
                String name = session.getUserName();
                twitterUserId.setText(name);
            }

            @Override
            public void failure(TwitterException exception) {
                Log.e("Twitter Login",""+exception.getMessage());
                Toast.makeText(MainActivity.this, "failed", Toast.LENGTH_SHORT).show();
            }
        });

        snapchat_login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SnapLogin.getAuthTokenManager(MainActivity.this).startTokenGrant();
                getUserNameSnapChat();
            }
        });

        SnapLogin.getLoginStateController(MainActivity.this).addOnLoginStateChangedListener(snapchatLoginButton());

    }

   private LoginStateController.OnLoginStateChangedListener snapchatLoginButton(){
       LoginStateController.OnLoginStateChangedListener loginStateController =  new LoginStateController.OnLoginStateChangedListener(){

           @Override
           public void onLoginSucceeded() {
               Toast.makeText(MainActivity.this,"login Successfull",Toast.LENGTH_SHORT).show();
               Log.e("Main Activity snapchat","login Successfull");

           }

           @Override
           public void onLoginFailed() {
               Toast.makeText(MainActivity.this,"Login Failed",Toast.LENGTH_SHORT).show();
           }

           @Override
           public void onLogout() {
               Toast.makeText(MainActivity.this,"Logout Success!",Toast.LENGTH_SHORT).show();
           }
       };
        return loginStateController;
    }

   void getUserNameSnapChat(){
       boolean isLogedIn = SnapLogin.isUserLoggedIn(MainActivity.this);
       if(isLogedIn) {
           String query = "{me{displayName}}";
           SnapLogin.fetchUserData(MainActivity.this, query, null, new FetchUserDataCallback() {
               @Override
               public void onSuccess(@Nullable UserDataResponse userDataResponse) {
                   if (userDataResponse == null || userDataResponse.getData() == null) {
                       Log.e("Main Activity snapchat ", "null");
                       return;
                   }

                   MeData meData = userDataResponse.getData().getMe();
                   if (meData == null) {
                       return;
                   }
                   snapchat_name.setText(meData.getDisplayName());
               }

               @Override
               public void onFailure(boolean b, int i) {
                   Toast.makeText(MainActivity.this, "Sorry problem with fetching try again", Toast.LENGTH_SHORT).show();
               }
           });
       }
   }
    private void getUserName(AccessToken accessToken) {
        GraphRequest request = GraphRequest.newMeRequest(accessToken, new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
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

    private void facebookClick(){
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        TwitterCore.getInstance().getSessionManager().clearActiveSession();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);

        twitterLogin.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }



}