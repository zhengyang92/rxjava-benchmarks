package co.chatsdk.firebase;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.firebase.database.DatabaseReference;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Date;

import co.chatsdk.core.base.AbstractCoreHandler;
import co.chatsdk.core.base.BaseHookHandler;
import co.chatsdk.core.dao.User;
import co.chatsdk.core.events.EventType;
import co.chatsdk.core.events.NetworkEvent;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.types.ChatError;
import co.chatsdk.core.types.FileUploadResult;
import co.chatsdk.core.utils.CrashReportingCompletableObserver;
import co.chatsdk.core.utils.DisposableList;
import co.chatsdk.firebase.wrappers.UserWrapper;
import io.reactivex.Completable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by benjaminsmiley-andrews on 02/05/2017.
 */

public class FirebaseCoreHandler extends AbstractCoreHandler {

    private DisposableList disposableList = new DisposableList();

    public FirebaseCoreHandler () {
        // When the user logs out, turn off all the existing listeners
        FirebaseEventHandler.shared().source()
                .filter(NetworkEvent.filterType(EventType.Logout))
                .subscribe(networkEvent -> disposableList.dispose());
    }

    public Completable pushUser () {
        return Single.create((SingleOnSubscribe<User>) e -> {

            // Check to see if the avatar URL is local or remote
            File avatar = new File(new URI(ChatSDK.currentUser().getAvatarURL()).getPath());
            Bitmap bitmap = BitmapFactory.decodeFile(avatar.getPath());

            if (new URL(ChatSDK.currentUser().getAvatarURL()).getHost() != null && bitmap != null && ChatSDK.upload() != null) {
                // Upload the image
                ChatSDK.upload().uploadImage(bitmap).subscribe(new Observer<FileUploadResult>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onNext(@NonNull FileUploadResult fileUploadResult) {
                        if (fileUploadResult.urlValid()) {
                            ChatSDK.currentUser().setAvatarURL(fileUploadResult.url);
                            ChatSDK.currentUser().update();
                            ChatSDK.events().source().onNext(NetworkEvent.userMetaUpdated(ChatSDK.currentUser()));
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable ex) {
                        ChatSDK.logError(ex);
                        e.onSuccess(ChatSDK.currentUser());
                    }

                    @Override
                    public void onComplete() {
                        e.onSuccess(ChatSDK.currentUser());
                    }
                });
            } else {
                e.onSuccess(ChatSDK.currentUser());
            }

        }).flatMapCompletable(user -> new UserWrapper(user).push()).subscribeOn(Schedulers.single());
    }

    public Completable setUserOnline() {

        User current = ChatSDK.currentUser();
        if (current != null && StringUtils.isNotEmpty(current.getEntityID())) {
            return UserWrapper.initWithModel(currentUserModel()).goOnline();
        }
        if(ChatSDK.hook() != null) {
            ChatSDK.hook().executeHook(BaseHookHandler.SetUserOnline, null);
        }

        return Completable.complete();
    }

    public Completable setUserOffline() {
        User current = ChatSDK.currentUser();
        if (current != null && StringUtils.isNotEmpty(current.getEntityID()))
        {
            // Update the last online figure then go offline
            return updateLastOnline()
                    .concatWith(UserWrapper.initWithModel(currentUserModel()).goOffline());
        }
        if(ChatSDK.hook() != null) {
            ChatSDK.hook().executeHook(BaseHookHandler.SetUserOffline, null);
        }

        return Completable.complete();
    }

    public void goOffline() {
        ChatSDK.core().save();
        disposableList.add(setUserOffline().subscribe(() -> DatabaseReference.goOffline()));
    }

    public void goOnline() {
        FirebasePaths.firebaseRawRef().child(".info/connected").addListenerForSingleValueEvent(new FirebaseEventListener().onValue((snapshot, hasValue) -> {
            if(hasValue) {
                Timber.v("Already online!");
            }
            else {
                DatabaseReference.goOnline();
            }
            setUserOnline().subscribe(new CrashReportingCompletableObserver(disposableList));
        }));
    }

    public Completable updateLastOnline () {
        return Completable.create(e -> {
            User currentUser = ChatSDK.currentUser();
            currentUser.setLastOnline(new Date());
            currentUser.update();
            e.onComplete();
        }).concatWith(pushUser()).subscribeOn(Schedulers.single());
    }

    public Single<Boolean> isOnline() {
        return Single.create((SingleOnSubscribe<Boolean>) e -> {
            if (ChatSDK.currentUser() == null) {
                e.onError(ChatError.getError(ChatError.Code.NULL, "Current user is null"));
                return;
            }

            FirebasePaths.userOnlineRef(ChatSDK.currentUser().getEntityID()).addListenerForSingleValueEvent(new FirebaseEventListener().onValue((snapshot, hasValue) -> {
                updateLastOnline().subscribe(new CrashReportingCompletableObserver(disposableList));
                e.onSuccess((Boolean) snapshot.getValue());
            }));

        }).subscribeOn(Schedulers.single());
    }

    public Completable userOn (final User user) {
        return Completable.create(e -> {
            final UserWrapper wrapper = new UserWrapper(user);
            disposableList.add(wrapper.onlineOn().doOnDispose(wrapper::onlineOff).subscribe(aBoolean -> {
                ChatSDK.events().source().onNext(NetworkEvent.userPresenceUpdated(user));
            }, e::onError));
            disposableList.add(wrapper.metaOn().doOnDispose(wrapper::metaOff).subscribe(user1 -> {
                ChatSDK.events().source().onNext(NetworkEvent.userMetaUpdated(user1));
                e.onComplete();
            }, e::onError));
        });
    }

    public void userOff (final User user) {
        UserWrapper wrapper = new UserWrapper(user);
        wrapper.onlineOff();
        wrapper.metaOff();
    }

    public void save () {

    }
}
