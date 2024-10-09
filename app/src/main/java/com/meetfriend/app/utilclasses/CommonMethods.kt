package contractorssmart.app.utilsclasses

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.util.Patterns
import android.view.Gravity
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.meetfriend.app.R
import com.meetfriend.app.responseclasses.Result
import java.net.URL
import java.util.*


object CommonMethods {
    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.lowercase(Locale.getDefault()).startsWith(manufacturer.lowercase(Locale.getDefault()))) {
            capitalize(model)
        } else {
            capitalize(manufacturer) + " " + model
        }
    }

    fun fromHtml(html: String?): Spanned? {
        return if (html == null) {
            // return an empty spannable if the html is null
            SpannableString("")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // FROM_HTML_MODE_LEGACY is the behaviour that was used for versions below android N
            // we are using this flag to give a consistent behaviour
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(html)
        }
    }

    fun isValidEmail(target: CharSequence?): Boolean {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    fun showToastMessageAtTop(context: Context, message: String) {
        try {
            val toast: Toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 150)
            toast.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun capitalize(s: String?): String {
        if (s == null || s.length == 0) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first) + s.substring(1)
        }
    }

    fun saveUserData(
        userData: Result, requireActivity: FragmentActivity, mediaUrl: String
    ) {
        PreferenceHandler.writeString(
            requireActivity, PreferenceHandler.USER_ID, "" + userData.id
        )
        if (userData.firstName != null) {
            PreferenceHandler.writeString(
                requireActivity, PreferenceHandler.FIRSTNAME, userData.firstName
            )
        }
        PreferenceHandler.writeString(
            requireActivity, PreferenceHandler.LASTNAME, userData.lastName
        )

        if (userData.city != null) {
            PreferenceHandler.writeString(
                requireActivity, PreferenceHandler.CITY, userData.city
            )
        }
        if (userData.education != null) {
            PreferenceHandler.writeString(
                requireActivity, PreferenceHandler.EDUCATION, userData.education
            )
        }

        if (userData.work != null) {
            PreferenceHandler.writeString(
                requireActivity, PreferenceHandler.WORK, userData.work
            )
        }
        if (userData.gender != null) {
            PreferenceHandler.writeString(
                requireActivity, PreferenceHandler.GENDER, userData.gender
            )
        }
        if (userData.dob != null) {
            PreferenceHandler.writeString(
                requireActivity, PreferenceHandler.DOB, userData.dob
            )
        }

        if (userData.relationship != null) {
            PreferenceHandler.writeString(
                requireActivity, PreferenceHandler.RELATIONSHIP, userData.relationship
            )
        }
        if (userData.hobbies != null) {
            PreferenceHandler.writeString(
                requireActivity, PreferenceHandler.HOBBIES, userData.hobbies
            )
        }
        if (userData.emailOrPhone != null) {
            PreferenceHandler.writeString(
                requireActivity, PreferenceHandler.EMAIL_OR_PHONE, userData.emailOrPhone
            )
        }
        if (userData.profilePhoto != null) {
            PreferenceHandler.writeString(
                requireActivity, PreferenceHandler.PROFILE_PHOTO, mediaUrl + userData.profilePhoto
            )
        }
        if (userData.coverPhoto != null) {
            PreferenceHandler.writeString(
                requireActivity, PreferenceHandler.COVER_PHOTO, mediaUrl + userData.coverPhoto
            )
        }

        if (userData.bio != null) {
            PreferenceHandler.writeString(
                requireActivity, PreferenceHandler.BIO, userData.bio
            )
        }

        if (userData.userName != null) {
            PreferenceHandler.writeString(
                requireActivity, PreferenceHandler.USER_NAME, userData.userName
            )
        }

    }

    fun setImage(imageView: ImageView, path: String) {
        if (path.equals("")) {
            return
        }
        try {

            Glide.with(imageView.context).load(URL(path))

                .placeholder(R.drawable.ic_placer_holder_image_new).diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean
                    ): Boolean {
                        return true
                    }

                    override fun onResourceReady(
                        resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean
                    ): Boolean {
                        imageView.setImageDrawable(resource)
                        return false
                    }
                }).into(imageView)


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}