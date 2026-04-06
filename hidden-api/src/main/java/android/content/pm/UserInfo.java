package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class UserInfo implements Parcelable {
    public static final Parcelable.Creator<UserInfo> CREATOR = new Parcelable.Creator<>() {

        public UserInfo createFromParcel(Parcel source) {
            throw new RuntimeException();
        }

        public UserInfo[] newArray(int size) {
            throw new RuntimeException();
        }
    };

    public int id;
    public String name;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        throw new RuntimeException();
    }
}
