package io.taucoin.android.interop;

import android.os.Parcel;
import android.os.Parcelable;

public class IpfsPeerInfo extends io.taucoin.ipfs.node.IpfsPeerInfo implements Parcelable {

    public IpfsPeerInfo(io.taucoin.ipfs.node.IpfsPeerInfo peerInfo) {

        super(peerInfo.getHost(), peerInfo.getPort(), peerInfo.getPeerId());
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        parcel.writeString(host);
        parcel.writeInt(port);
        parcel.writeString(peerId);
    }

    public static final Parcelable.Creator<IpfsPeerInfo> CREATOR = new Parcelable.Creator<IpfsPeerInfo>() {

        public IpfsPeerInfo createFromParcel(Parcel in) {

            return new IpfsPeerInfo(in);
        }

        public IpfsPeerInfo[] newArray(int size) {

            return new IpfsPeerInfo[size];
        }
    };

    private IpfsPeerInfo(Parcel in) {

        super("", -1, "");

        host = in.readString();
        port = in.readInt();
        peerId = in.readString();
    }
}
