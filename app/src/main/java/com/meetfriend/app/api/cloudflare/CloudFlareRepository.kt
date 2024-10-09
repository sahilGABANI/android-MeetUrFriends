package com.meetfriend.app.api.cloudflare

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.cloudflare.model.CloudFlareVideoInfo
import com.meetfriend.app.api.cloudflare.model.CloudFlareVideoStatus
import com.meetfriend.app.api.cloudflare.model.ImageToCloudFlare
import com.meetfriend.app.api.cloudflare.model.UploadImageCloudFlareResponse
import com.meetfriend.app.api.cloudflare.model.UploadVideoCloudFlareResponse
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForChat
import com.meetfriend.app.utils.ProgressRequestBody
import io.reactivex.Single
import io.tus.java.client.ProtocolException
import io.tus.java.client.TusClient
import io.tus.java.client.TusExecutor
import io.tus.java.client.TusURLMemoryStore
import io.tus.java.client.TusUpload
import io.tus.java.client.TusUploader
import okhttp3.MultipartBody
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URL

class CloudFlareRepository(
    private val cloudFlareRetrofitAPI: CloudFlareRetrofitAPI,
) {
    companion object {
        const val ONE_HUNDRED = 100
        const val TWO_HUNDRED_FIFTY_SIX = 256
        const val ONE_THOUSANT_TWENTY_FOUR = 1024
    }
    private var data: MutableLiveData<Double> = MutableLiveData<Double>()

    private var tusClient: TusClient? = null
    private var tusUploader: TusUploader? = null

    private val meetFriendResponseConverter: MeetFriendResponseConverter =
        MeetFriendResponseConverter()

    fun getCloudFlareConfig(): Single<MeetFriendResponseForChat<CloudFlareConfig>> {
        return cloudFlareRetrofitAPI.getCloudFlareConfig()
            .doAfterSuccess {}
            .flatMap {
                meetFriendResponseConverter.convertToSingleWithFullResponseForCreateChatRoom(
                    it
                )
            }
    }

    fun uploadImageToCloudFlare(
        imageToCloudFlare: ImageToCloudFlare
    ): Single<UploadImageCloudFlareResponse> {
        val progressFilePart = ProgressRequestBody(
            imageToCloudFlare.filePart.body,
            object : ProgressRequestBody.UploadCallbacks {
                override fun onProgressUpdate(percentage: Double) {
                    if (imageToCloudFlare.totalRequests != null && imageToCloudFlare.totalRequests > 1) {
                        Timber.tag(
                            "UpdateProcess"
                        ).d(
                            "percentage: $percentage, totalRequests: " +
                                "${imageToCloudFlare.totalRequests}," +
                                " currentRequestIndex: ${imageToCloudFlare.currentRequestIndex}"
                        )
                        val adjustedPercentage = adjustPercentage(
                            percentage,
                            imageToCloudFlare.totalRequests,
                            imageToCloudFlare.currentRequestIndex ?: 0
                        )
                        data.postValue(adjustedPercentage)
                    } else {
                        data.postValue(percentage)
                    }
                }
            }
        )

        val progressPart = MultipartBody.Part.createFormData(
            "file",
            imageToCloudFlare.fileName,
            progressFilePart
        )

        return cloudFlareRetrofitAPI.uploadImageToCloudFlare(
            imageToCloudFlare.apiUrl,
            imageToCloudFlare.authToken,
            if (!imageToCloudFlare.fileName.isNullOrEmpty()) {
                progressPart
            } else {
                imageToCloudFlare.filePart
            }
        )
            .doFinally {
                progressFilePart.clearCallback() // Clear the callback to avoid leaks
            }
            .map { it }
    }

    private fun adjustPercentage(
        percentage: Double,
        totalRequests: Int,
        currentRequestIndex: Int
    ): Double {
        val new = currentRequestIndex * ONE_HUNDRED
        return (percentage + new) / totalRequests
    }

    fun uploadVideoToCloudFlare(
        apiUrl: String,
        authToken: String,
        filePart: MultipartBody.Part
    ): Single<UploadVideoCloudFlareResponse> {
        return cloudFlareRetrofitAPI.uploadVideoToCloudFlare(apiUrl, authToken, filePart)
            .doAfterSuccess {}
            .map { it }
    }

    fun getUploadVideoDetails(apiUrl: String, authToken: String): Single<CloudFlareVideoInfo> {
        return cloudFlareRetrofitAPI.getUploadVideoDetails(apiUrl, authToken)
            .doAfterSuccess {}
            .map { it.result }
    }

    fun uploadVideoUsingTus(apiUrl: String, authToken: String, file: File): Single<String> {
        return Single.create { singleEmitter ->
            val headers = mutableMapOf<String, String>()
            headers["Authorization"] = authToken
            tusClient = TusClient()
            tusClient?.uploadCreationURL = URL(apiUrl)
            tusClient?.headers = headers
            tusClient?.enableResuming(TusURLMemoryStore())
            val upload = TusUpload(file)
            val executor: TusExecutor = object : TusExecutor() {
                @Throws(ProtocolException::class, IOException::class)
                override fun makeAttempt() {
                    tusUploader = tusClient?.resumeOrCreateUpload(upload)
                    tusUploader?.let { tusUploader ->
                        tusUploader.chunkSize = TWO_HUNDRED_FIFTY_SIX * ONE_THOUSANT_TWENTY_FOUR
                        do {
                            val totalBytes = upload.size
                            val bytesUploaded = tusUploader.offset
                            val progress = bytesUploaded.toDouble() / totalBytes * ONE_HUNDRED
                            Timber.i("Upload at %06.2f%%.\n", progress)

                            data.postValue(progress)
                        } while (tusUploader.uploadChunk() > -1)
                        tusUploader.finish()
                        Timber.i("Upload finished.")
                        Timber.i(
                            System.out.format(
                                "Upload available at: %s",
                                tusUploader.uploadURL.toString()
                            ).toString()
                        )
                        singleEmitter.onSuccess(tusUploader.uploadURL.toString())
                    }
                }
            }
            executor.makeAttempts()
        }
    }

    fun getLiveData(): LiveData<Double> {
        return data
    }

    fun getUploadVideoStatus(apiUrl: String, authToken: String): Single<CloudFlareVideoStatus> {
        return cloudFlareRetrofitAPI.getUploadVideoStatus(apiUrl, authToken)
            .doAfterSuccess {}
            .map { it }
    }
}
