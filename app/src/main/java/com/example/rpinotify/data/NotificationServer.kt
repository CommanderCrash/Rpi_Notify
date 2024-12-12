package com.example.rpinotify.data

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import java.util.UUID

@Keep
data class NotificationServer(
    val id: String = UUID.randomUUID().toString(),
    val ipAddress: String,
    var name: String = "",
    var status: String = "Disconnected",
    var lastPing: Long = 0,
    val port: Int = 5556,
    var useUdp: Boolean = true,
    var pingMs: Long = 0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: UUID.randomUUID().toString(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "Disconnected",
        parcel.readLong(),
        parcel.readInt(),
        parcel.readInt() == 1,
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(ipAddress)
        parcel.writeString(name)
        parcel.writeString(status)
        parcel.writeLong(lastPing)
        parcel.writeInt(port)
        parcel.writeInt(if (useUdp) 1 else 0)
        parcel.writeLong(pingMs)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<NotificationServer> {
        override fun createFromParcel(parcel: Parcel): NotificationServer {
            return NotificationServer(parcel)
        }

        override fun newArray(size: Int): Array<NotificationServer?> {
            return arrayOfNulls(size)
        }
    }
}