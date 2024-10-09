package com.meetfriend.app.ui.monetization.earnings

import android.os.Parcel
import android.os.Parcelable
import com.google.android.material.datepicker.CalendarConstraints

class DateValidatorFiftyYearRange(private val start: Long, private val end: Long) :
    CalendarConstraints.DateValidator {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(p0: Parcel, p1: Int) {
        p0?.writeLong(start)
        p0?.writeLong(end)
    }

    override fun isValid(date: Long): Boolean {
        return date in start..end
    }

    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else other is DateValidatorFiftyYearRange
    }

    override fun hashCode(): Int {
        val hashedFields = arrayOf<Any>(start, end)
        return hashedFields.contentHashCode()
    }

    companion object CREATOR : Parcelable.Creator<DateValidatorFiftyYearRange> {
        override fun createFromParcel(parcel: Parcel): DateValidatorFiftyYearRange {
            return DateValidatorFiftyYearRange(parcel)
        }

        override fun newArray(size: Int): Array<DateValidatorFiftyYearRange?> {
            return arrayOfNulls(size)
        }
    }

    constructor(parcel: Parcel) : this(parcel.readLong(), parcel.readLong())
}