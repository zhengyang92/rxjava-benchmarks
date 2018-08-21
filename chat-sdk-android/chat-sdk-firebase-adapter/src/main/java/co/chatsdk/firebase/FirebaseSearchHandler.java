package co.chatsdk.firebase;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;

import co.chatsdk.core.dao.Keys;
import co.chatsdk.core.dao.User;
import co.chatsdk.core.defines.FirebaseDefines;
import co.chatsdk.core.handlers.SearchHandler;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.types.ChatError;
import co.chatsdk.firebase.wrappers.UserWrapper;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by benjaminsmiley-andrews on 24/05/2017.
 */

public class FirebaseSearchHandler implements SearchHandler {

    public Observable<User> usersForIndex(final String value) {
        return usersForIndexes(value, Keys.Name, Keys.Email, Keys.Phone);
    }

    public Observable<User> usersForIndexes(final String value, final String... indexes) {
        ArrayList<Observable<User>> observables = new ArrayList<>();
        for (String index : indexes) {
            observables.add(usersForIndex(value, index));
        }
        return Observable.merge(observables);
    }

    public Observable<User> usersForIndex(final String value, final String index) {
        return Observable.create((ObservableOnSubscribe<User>) e -> {

            if (StringUtils.isBlank(value))
            {
                e.onError(ChatError.getError(ChatError.Code.NULL, "Value is blank"));
                return;
            }

            final Query query = FirebasePaths.usersRef()
                    .orderByChild(Keys.Meta + '/' + index)
                    .startAt(value)
                    .limitToFirst(FirebaseDefines.NumberOfUserToLoadForIndex);
            query.keepSynced(true);

            query.addListenerForSingleValueEvent(new FirebaseEventListener().onValue((snapshot, hasValue) -> {
                if (hasValue) {
                    Object valueObject = snapshot.getValue();
                    if (valueObject instanceof HashMap) {
                        for (Object key : ((HashMap) valueObject).keySet()) {
                            if (key instanceof String) {
                                DataSnapshot userSnapshot = snapshot.child((String) key);

                                if (userSnapshot.hasChild(Keys.Meta)) {
                                    DataSnapshot meta = userSnapshot.child(Keys.Meta);
                                    if (meta.hasChild(index)) {
                                        String childValue = (String) meta.child(index).getValue();
                                        if (childValue.toLowerCase().contains(value.toLowerCase())) {
                                            final UserWrapper wrapper = new UserWrapper(userSnapshot);
                                            if (!wrapper.getModel().equals(ChatSDK.currentUser()) && !ChatSDK.contact().exists(wrapper.getModel())) {
                                                e.onNext(wrapper.getModel());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                e.onComplete();
            }).onCancelled(error -> {
                e.onError(new Throwable(error.getMessage()));
//                e.onComplete();
            }));

            e.setDisposable(new Disposable() {
                @Override
                public void dispose() {
                    //query.removeEventListener(listener);
                }

                @Override
                public boolean isDisposed() {
                    return false;
                }
            });
        }).subscribeOn(Schedulers.single());
    }

    public static String processForQuery(String query){
        return StringUtils.isBlank(query) ? "" : query.replace(" ", "").toLowerCase();
    }

}
