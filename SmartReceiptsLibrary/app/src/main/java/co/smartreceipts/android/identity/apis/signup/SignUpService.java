package co.smartreceipts.android.identity.apis.signup;

import android.support.annotation.NonNull;

import co.smartreceipts.android.identity.apis.login.LoginResponse;
import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SignUpService {

    @POST("api/users/sign_up")
    Observable<LoginResponse> signUp(@NonNull @Body SignUpPayload signUpPayload);
}
