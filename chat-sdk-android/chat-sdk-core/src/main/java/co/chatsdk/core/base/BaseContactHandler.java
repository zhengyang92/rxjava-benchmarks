package co.chatsdk.core.base;

import java.util.ArrayList;
import java.util.List;

import co.chatsdk.core.dao.User;
import co.chatsdk.core.handlers.ContactHandler;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.types.ConnectionType;
import io.reactivex.Completable;

/**
 * Created by benjaminsmiley-andrews on 24/05/2017.
 */

public class BaseContactHandler implements ContactHandler {

    @Override
    public List<User> contacts() {
        if(ChatSDK.currentUser() != null) {
            return ChatSDK.currentUser().getContacts();
        }
        return new ArrayList<>();
    }

    @Override
    public boolean exists(User user) {
        for (User u : contacts()) {
            if (u.getEntityID().equals(user.getEntityID())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<User> contactsWithType(ConnectionType type) {
        if(ChatSDK.currentUser() != null) {
            return ChatSDK.currentUser().getContacts(type);
        }
        return new ArrayList<>();
    }

    @Override
    public Completable addContact(User user, ConnectionType type) {
        if(ChatSDK.currentUser() != null && !user.isMe()) {
            ChatSDK.currentUser().addContact(user, type);
            ChatSDK.core().userOn(user);
        }
        return Completable.complete();
    }

    @Override
    public Completable deleteContact(User user, ConnectionType type) {
        if(ChatSDK.currentUser() != null && !user.isMe()) {
            ChatSDK.currentUser().deleteContact(user, type);
            ChatSDK.core().userOff(user);
        }
        return Completable.complete();
    }

    @Override
    public Completable addContacts(ArrayList<User> users, ConnectionType type) {
        ArrayList<Completable> completables = new ArrayList<>();
        for(User user : users) {
            completables.add(addContact(user, type));
        }
        return Completable.concat(completables);
    }

    @Override
    public Completable deleteContacts(ArrayList<User> users, ConnectionType type) {
        ArrayList<Completable> completables = new ArrayList<>();
        for(User user : users) {
            completables.add(addContact(user, type));
        }
        return Completable.concat(completables);
    }

}
