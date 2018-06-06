package com.mstarc.wechat.wearwechat.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.mstarc.wechat.wearwechat.utils.StringUtil;
import com.mstarc.wechat.wearwechat.utils.WxHome;

import java.util.List;

public class Contact implements Parcelable, Comparable<Contact> {
    public static final Creator<Contact> CREATOR = new Creator() {
        public Contact createFromParcel(Parcel paramAnonymousParcel) {
            return new Contact(paramAnonymousParcel);
        }

        public Contact[] newArray(int paramAnonymousInt) {
            return new Contact[paramAnonymousInt];
        }
    };
    public int ContactFlag;
    public String DisplayName;
    public String HeadImgUrl;
    public String KeyWord;
    public int MemberCount;
    public List<Contact> MemberList;
    public String NickName;
    public String PYQuanPin = "";
    public String RemarkName;
    public String RemarkPYQuanPin = "";
    public int Statues;
    public String UserName;
    public int VerifyFlag;

    public Contact() {
    }

    public Contact(Parcel paramParcel) {
        this.UserName = paramParcel.readString();
        this.NickName = paramParcel.readString();
        this.HeadImgUrl = paramParcel.readString();
        this.ContactFlag = paramParcel.readInt();
        this.VerifyFlag = paramParcel.readInt();
        this.RemarkName = paramParcel.readString();
        this.DisplayName = paramParcel.readString();
        this.Statues = paramParcel.readInt();
    }

    public int compareTo(Contact paramContact) {
        if (this.ContactFlag != paramContact.ContactFlag)
            return this.ContactFlag - paramContact.ContactFlag;
        String str1 = this.RemarkPYQuanPin.toLowerCase();
        if (StringUtil.isNullOrEmpty(this.RemarkPYQuanPin))
            str1 = this.PYQuanPin.toLowerCase();
        String str2 = paramContact.RemarkPYQuanPin.toLowerCase();
        if (StringUtil.isNullOrEmpty(paramContact.RemarkPYQuanPin))
            str2 = paramContact.PYQuanPin.toLowerCase();
        return str1.compareTo(str2);
    }

    public int describeContents() {
        return 0;
    }


    public String getShowName() {
        if (StringUtil.notNullOrEmpty(this.DisplayName))
            return this.DisplayName;
        if (StringUtil.notNullOrEmpty(this.RemarkName))
            return this.RemarkName;
        if (StringUtil.notNullOrEmpty(this.NickName))
            return this.NickName;
        if (WxHome.isGroupUserName(this.UserName)) {
            if (this.MemberList == null)
                return "";
            StringBuilder localStringBuilder = new StringBuilder();
            int i = 0;
            if ((i < 3) && (i < this.MemberList.size())) {
                localStringBuilder.append((this.MemberList.get(i)).getShowName());
                if (i < -1 + this.MemberList.size()) {
                    if (i != 2)

                        localStringBuilder.append("...");
                }
                while (true) {
                    i++;
                    localStringBuilder.append("、");
                    break;


                }
            }
            return localStringBuilder.toString();
        }
        return "";
    }

    public boolean isMuted() {
        Log.d("Contact", "isMuted, NickName=" + this.NickName + " Statues=" + this.Statues + " ContactFlag=" + this.ContactFlag);
        if ((WxHome.isGroupUserName(this.UserName)) && (this.Statues == 0)) ;
        while ((this.ContactFlag >= 500) && (this.ContactFlag < 600))
            return true;
        return false;
    }

    public void writeToParcel(Parcel paramParcel, int paramInt) {
        paramParcel.writeString(this.UserName);
        paramParcel.writeString(this.NickName);
        paramParcel.writeString(this.HeadImgUrl);
        paramParcel.writeInt(this.ContactFlag);
        paramParcel.writeInt(this.VerifyFlag);
        paramParcel.writeString(this.RemarkName);
        paramParcel.writeString(this.DisplayName);
        paramParcel.writeInt(this.Statues);
    }
}

