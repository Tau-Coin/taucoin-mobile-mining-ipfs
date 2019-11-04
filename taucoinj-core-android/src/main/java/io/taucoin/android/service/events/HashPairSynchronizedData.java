package io.taucoin.android.service.events;

import android.os.Parcel;
import android.os.Parcelable;

public class HashPairSynchronizedData extends EventData {

    public long number;

    public HashPairSynchronizedData(long number) {

        super();
        this.number = number;
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        super.writeToParcel(parcel, i);
        parcel.writeLong(number);
    }

    public static final Parcelable.Creator<HashPairSynchronizedData> CREATOR = new Parcelable.Creator<HashPairSynchronizedData>() {

        public HashPairSynchronizedData createFromParcel(Parcel in) {

            return new HashPairSynchronizedData(in);
        }

        public HashPairSynchronizedData[] newArray(int size) {

            return new HashPairSynchronizedData[size];
        }
    };

    private HashPairSynchronizedData(Parcel in) {

        super(in);
        number = in.readLong();
    }
}
