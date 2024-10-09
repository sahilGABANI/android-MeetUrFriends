package com.meetfriend.app.ui.camerakit.model

import android.content.Context
import com.meetfriend.app.api.post.model.LinkAttachmentDetails
import com.meetfriend.app.api.post.model.MultipleImageDetails

sealed class StandardPromptDialogState {
    object CloseDialogClick : StandardPromptDialogState()
    object AcceptClick : StandardPromptDialogState()
}

sealed class AgeRestrictionDialogState {
    object CloseDialogClick : AgeRestrictionDialogState()
    object AdultClick : AgeRestrictionDialogState()
    object ChildClick : AgeRestrictionDialogState()
}

sealed class ChildDialogState {
    object CloseDialogClick : ChildDialogState()
    object IAmGuardianClick : ChildDialogState()
}

sealed class TermsOfServiceState {
    object CloseDialogClick : TermsOfServiceState()
    object AcceptClick : TermsOfServiceState()
}
data class LauncherGetIntent(
    val context: Context,
    val path: String,
    val mimeType: String,
    val playBackSpeed: Float,
    val isShorts: Boolean = false,
    val isChallenge: Boolean = false,
    val isStory: Boolean = false,
    val linkAttachmentDetails: LinkAttachmentDetails?,
    val listOfMultipleMedia: ArrayList<MultipleImageDetails>,
    val tagName: String? = null,
)
