package co.smartreceipts.android.aws.cognito;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.google.common.base.Preconditions;
import com.hadisatrio.optional.Optional;

import javax.inject.Inject;

import co.smartreceipts.android.di.scopes.ApplicationScope;
import co.smartreceipts.android.identity.IdentityManager;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;


@ApplicationScope
public class CognitoManager {

    private final Context context;
    private final IdentityManager identityManager;
    private final CognitoIdentityProvider cognitoIdentityProvider;
    private final Scheduler subscribeOnScheduler;
    private final PublishSubject<Object> retryErrorsOnSubscribePredicate = PublishSubject.create();
    private final ReplaySubject<Optional<CognitoCachingCredentialsProvider>> cachingCredentialsProviderReplaySubject = ReplaySubject.createWithSize(1);
    private Disposable cachingCredentialsProviderDisposable;

    @Inject
    public CognitoManager(@NonNull Context context,
                          @NonNull IdentityManager identityManager,
                          @NonNull CognitoIdentityProvider cognitoIdentityProvider) {
        this(context, identityManager, cognitoIdentityProvider, Schedulers.io());
    }

    @VisibleForTesting
    CognitoManager(@NonNull Context context,
                   @NonNull IdentityManager identityManager,
                   @NonNull CognitoIdentityProvider cognitoIdentityProvider,
                   @NonNull Scheduler subscribeOnScheduler) {
        this.context = Preconditions.checkNotNull(context);
        this.identityManager = Preconditions.checkNotNull(identityManager);
        this.cognitoIdentityProvider = Preconditions.checkNotNull(cognitoIdentityProvider);
        this.subscribeOnScheduler = Preconditions.checkNotNull(subscribeOnScheduler);
    }

    public void initialize() {
        cachingCredentialsProviderDisposable = identityManager.isLoggedInStream()
                .subscribeOn(subscribeOnScheduler)
                .flatMap(isLoggedIn -> {
                    if (isLoggedIn) {
                        return cognitoIdentityProvider.prefetchCognitoTokenIfNeeded()
                                .retryWhen(throwableFlowable -> throwableFlowable
                                        .flatMap(throwable -> retryErrorsOnSubscribePredicate.toFlowable( BackpressureStrategy.MISSING)))
                                .map(cognitoOptional -> {
                                    final SmartReceiptsAuthenticationProvider authenticationProvider = new SmartReceiptsAuthenticationProvider(cognitoIdentityProvider, getRegions());
                                    return Optional.of(new CognitoCachingCredentialsProvider(context, authenticationProvider, getRegions()));
                                })
                                .onErrorReturn(throwable -> Optional.absent())
                                .toObservable();
                    } else {
                        return Observable.just(Optional.<CognitoCachingCredentialsProvider>absent());
                    }
                })
                .subscribe(cognitoCachingCredentialsProviderOptional -> {
                            cachingCredentialsProviderReplaySubject.onNext(cognitoCachingCredentialsProviderOptional);
                            if (cognitoCachingCredentialsProviderOptional.isPresent()) {
                                cachingCredentialsProviderReplaySubject.onComplete();
                                if (cachingCredentialsProviderDisposable != null) {
                                    cachingCredentialsProviderDisposable.dispose();
                                }
                            }
                        },
                        cachingCredentialsProviderReplaySubject::onError,
                        cachingCredentialsProviderReplaySubject::onComplete);
    }

    /**
     * @return an {@link Optional} instance of the {@link CognitoCachingCredentialsProvider}. Once we
     * fetch a valid entry, this should be treated as a singleton for the lifetime of the parent
     * {@link CognitoManager} object, since we use a replay subject
     */
    @NonNull
    public Observable<Optional<CognitoCachingCredentialsProvider>> getCognitoCachingCredentialsProvider() {
        return cachingCredentialsProviderReplaySubject
                .doOnSubscribe(disposable -> {
                    // Any time we subscribe, let's see if we can resolve any latent errors
                    retryErrorsOnSubscribePredicate.onNext(new Object());
                });
    }

    @NonNull
    public Regions getRegions() {
        return Regions.US_EAST_1;
    }

}
