package com.meetfriend.app.ui.mediapicker.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.meetfriend.app.R
import com.meetfriend.app.ui.mediapicker.adapter.PhotoAlbumListAdapter
import com.meetfriend.app.ui.mediapicker.interfaces.PhotoAlbumAdapterCallback
import com.meetfriend.app.ui.mediapicker.model.AlbumPhotoModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.*

class PhotoAlbumListBSDialogFragment(
    private val albumPhotoItemArrayList: ArrayList<AlbumPhotoModel>
) : BottomSheetDialogFragment(), View.OnClickListener, PhotoAlbumAdapterCallback {

    private val itemClickSubject: PublishSubject<Int> = PublishSubject.create()
    val itemClick: Observable<Int> = itemClickSubject.hide()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, R.layout.dialog_bs_fragment_album_list, null)
        dialog.setContentView(contentView)
        context?.let {
            (contentView.parent as View).setBackgroundColor(
                ContextCompat.getColor(
                    it,
                    R.color.color_transparent
                )
            )
        }

        val bottomSheetDialog = dialog as BottomSheetDialog
        val bottomSheet =
            bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        if (bottomSheet != null) {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        listenToViewEvents(contentView)
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    private lateinit var rvAlbumList: RecyclerView
    private lateinit var photoAlbumItemsAdapter: PhotoAlbumListAdapter

    private lateinit var ivClose: AppCompatImageView

    private fun listenToViewEvents(view: View) {
        rvAlbumList = view.findViewById(R.id.rvAlbumList) as RecyclerView
        rvAlbumList.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

        photoAlbumItemsAdapter = PhotoAlbumListAdapter(view.context, albumPhotoItemArrayList, this)
        rvAlbumList.adapter = photoAlbumItemsAdapter

        ivClose = view.findViewById(R.id.ivClose) as AppCompatImageView

        ivClose.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivClose -> {
                dismiss()
            }
        }
    }

    override fun onPhotoAlbumItemClick(mPos: Int) {
        itemClickSubject.onNext(mPos)
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}