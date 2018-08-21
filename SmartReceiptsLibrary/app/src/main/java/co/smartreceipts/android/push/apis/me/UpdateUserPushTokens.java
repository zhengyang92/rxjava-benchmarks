package co.smartreceipts.android.push.apis.me;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class UpdateUserPushTokens implements Serializable {

    @SerializedName("registration_ids")
    private List<String> registrationIds;

    public UpdateUserPushTokens(@NonNull List<String> registrationIds) {
        this.registrationIds = Preconditions.checkNotNull(registrationIds);
    }

    @Nullable
    public List<String> getRegistrationIds() {
        return registrationIds;
    }
}
