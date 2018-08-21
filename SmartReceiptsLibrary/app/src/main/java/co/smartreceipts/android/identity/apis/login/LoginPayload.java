package co.smartreceipts.android.identity.apis.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class LoginPayload implements Serializable {

    @SerializedName("login_params")
    private final UserCredentialsPayload userCredentialsPayload;

    public LoginPayload(@NonNull UserCredentialsPayload userCredentialsPayload) {
        this.userCredentialsPayload = userCredentialsPayload;
    }

    @Nullable
    public UserCredentialsPayload getUserCredentialsPayload() {
        return userCredentialsPayload;
    }
}
