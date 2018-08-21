package co.smartreceipts.android.di;

import co.smartreceipts.android.SmartReceiptsApplication;
import co.smartreceipts.android.di.scopes.ApplicationScope;
import co.smartreceipts.android.push.PushManager;
import dagger.Component;

@ApplicationScope
@Component(modules = {
        FlavorModule.class,
        GlobalBindingModule.class,
        BaseAppModule.class
})
public interface AppComponent {

    SmartReceiptsApplication inject(SmartReceiptsApplication application);

    PushManager providePushManager();
}
