package io.taucoin.android.interop;

import android.os.Parcel;
import android.os.Parcelable;

public class IpfsHomeNodeInfo extends io.taucoin.ipfs.node.IpfsHomeNodeInfo implements Parcelable {

    public IpfsHomeNodeInfo(io.taucoin.ipfs.node.IpfsHomeNodeInfo info) {

        super(info.getId(), info.getVersion());
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        parcel.writeString(id);
        parcel.writeString(version);
    }

    public static final Parcelable.Creator<IpfsHomeNodeInfo> CREATOR = new Parcelable.Creator<IpfsHomeNodeInfo>() {

        public IpfsHomeNodeInfo createFromParcel(Parcel in) {

            return new IpfsHomeNodeInfo(in);
        }

        public IpfsHomeNodeInfo[] newArray(int size) {

            return new IpfsHomeNodeInfo[size];
        }
    };

    private IpfsHomeNodeInfo(Parcel in) {
        super(in.readString(), in.readString());
    }
}
