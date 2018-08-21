package co.smartreceipts.android.identity.widget.login;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;

import javax.inject.Inject;

import co.smartreceipts.android.R;
import co.smartreceipts.android.apis.SmartReceiptsApiErrorResponse;
import co.smartreceipts.android.apis.SmartReceiptsApiException;
import co.smartreceipts.android.di.scopes.ApplicationScope;
import co.smartreceipts.android.identity.IdentityManager;
import co.smartreceipts.android.identity.apis.login.LoginType;
import co.smartreceipts.android.identity.apis.login.UserCredentialsPayload;
import co.smartreceipts.android.utils.log.Logger;
import co.smartreceipts.android.widget.model.UiIndicator;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.ReplaySubject;
import retrofit2.HttpException;

@ApplicationScope
public class LoginInteractor {

    private static final int INVALID_CREDENTIALS_CODE = 401;
    private static final int ACCOUNT_ALREADY_EXISTS_CODE = 420;

    private final Context context;
    private final IdentityManager identityManager;
    private UserCredentialsPayload userCredentialsPayload;
    private ReplaySubject<UiIndicator<String>> uiIndicatorReplaySubject;

    @Inject
    public LoginInteractor(@NonNull Context context, @NonNull IdentityManager identityManager) {
        this.context = Preconditions.checkNotNull(context.getApplicationContext());
        this.identityManager = Preconditions.checkNotNull(identityManager);
    }

    /**
     * Initiates a login/sign-up request for a given {@link UserCredentialsPayload}
     *
     * @param userCredentialsPayload the payload to use to login/sign-up
     * @return an {@link Observable}, which will emit {@link UiIndicator} results based on the state
     * of the request
     */
    @NonNull
    public synchronized Observable<UiIndicator<String>> loginOrSignUp(@NonNull final UserCredentialsPayload userCredentialsPayload) {
        Logger.info(this, "Initiating user login (or sign up)");

        if (this.uiIndicatorReplaySubject == null) {
            this.userCredentialsPayload = userCredentialsPayload;
            this.uiIndicatorReplaySubject = ReplaySubject.create();

            identityManager.logInOrSignUp(userCredentialsPayload)
                    .map(loginResponse -> UiIndicator.success(getSuccessMessage(userCredentialsPayload)))
                    .onErrorReturn(throwable -> UiIndicator.error(getErrorMessage(userCredentialsPayload, throwable)))
                    .startWith(UiIndicator.loading())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(uiIndicatorReplaySubject);
        }
        return uiIndicatorReplaySubject;
    }

    /**
     * @return the last {@link UserCredentialsPayload} submitted to {@link #loginOrSignUp(UserCredentialsPayload)}
     * in a {@link Maybe} observable or {@link Maybe#empty()} if one has either not be made or if
     * {@link #onLoginResultsConsumed()} was previously called
     */
    @NonNull
    public synchronized Maybe<UserCredentialsPayload> getLastUserCredentialsPayload() {
        if (userCredentialsPayload != null) {
            return Maybe.just(userCredentialsPayload);
        } else {
            return Maybe.empty();
        }
    }

    /**
     * Clears out the results of our previous login request after it has been consumed by the UI
     */
    public synchronized void onLoginResultsConsumed() {
        userCredentialsPayload = null;
        uiIndicatorReplaySubject = null;
    }

    @NonNull
    private String getSuccessMessage(@NonNull final UserCredentialsPayload userCredentialsPayload) {
        if (userCredentialsPayload.getLoginType() == LoginType.LogIn) {
            return context.getString(R.string.login_success_toast);
        } else {
            return context.getString(R.string.sign_up_success_toast);
        }
    }

    @NonNull
    private String getErrorMessage(@NonNull final UserCredentialsPayload userCredentialsPayload, @NonNull Throwable throwable) {
        String errorMessage;
        if (userCredentialsPayload.getLoginType() == LoginType.LogIn) {
            errorMessage = context.getString(R.string.login_failure_toast);
        } else {
            errorMessage = context.getString(R.string.sign_up_failure_toast);
        }

        if (throwable instanceof SmartReceiptsApiException) {
            final SmartReceiptsApiException smartReceiptsApiException = (SmartReceiptsApiException) throwable;
            final SmartReceiptsApiErrorResponse errorResponse = smartReceiptsApiException.getErrorResponse();
            if (errorResponse != null && errorResponse.getErrors() != null && !errorResponse.getErrors().isEmpty()) {
                errorMessage = errorResponse.getErrors().get(0);
            }
        } else if (throwable instanceof HttpException) {
            final HttpException httpException = (HttpException) throwable;
            if (httpException.code() == INVALID_CREDENTIALS_CODE) {
                errorMessage = context.getString(R.string.login_failure_credentials_toast);
            }
            else if (httpException.code() == ACCOUNT_ALREADY_EXISTS_CODE) {
                errorMessage = context.getString(R.string.sign_up_failure_account_exists_toast);
            }
        }
        return errorMessage;
    }

}
