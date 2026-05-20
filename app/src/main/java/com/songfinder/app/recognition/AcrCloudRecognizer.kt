package com.songfinder.app.recognition

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import com.songfinder.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

/**
 * ACRCloud ses tanıma:
 * 1. Mikrofon → PCM ses kaydeder (RECORD_DURATION_SEC saniye)
 * 2. HMAC-SHA1 imzası oluşturur
 * 3. ACRCloud REST API'ye gönderir
 * 4. Şarkı adı + sanatçı döner
 */
class AcrCloudRecognizer {

    companion object {
        private const val TAG               = "ACRCloud"
        private const val RECORD_DURATION_SEC = 10
        private const val SAMPLE_RATE       = 8000
        private const val CHANNEL_CONFIG    = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT      = AudioFormat.ENCODING_PCM_16BIT
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Volatile var isRecording = false
        private set

    data class RecognitionResult(
        val title: String,
        val artist: String,
        val album: String = "",
        val success: Boolean,
        val errorMsg: String = ""
    )

    /**
     * Mikrofonu aç → kaydet → ACRCloud'a gönder → sonuç döndür
     * Bu fonksiyon Dispatchers.IO'da çalışmalı
     */
    suspend fun recognizeSong(
        onProgress: (String) -> Unit = {}
    ): RecognitionResult = withContext(Dispatchers.IO) {

        // ── 1. Mikrofon kaydı ──────────────────────────────────────────────
        onProgress("🎙️ Dinleniyor…")
        val pcmData = recordAudio(onProgress) ?: return@withContext RecognitionResult(
            title = "", artist = "", success = false,
            errorMsg = "Mikrofon başlatılamadı"
        )

        // ── 2. ACRCloud'a gönder ───────────────────────────────────────────
        onProgress("🔍 Şarkı tanınıyor…")
        return@withContext sendToAcrCloud(pcmData)
    }

    private fun recordAudio(onProgress: (String) -> Unit): ByteArray? {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        ).coerceAtLeast(4096)

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            ).also { if (it.state != AudioRecord.STATE_INITIALIZED) return null }
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecord init hatası: ${e.message}")
            return null
        }

        val totalSamples = SAMPLE_RATE * RECORD_DURATION_SEC
        val audioData    = ByteArray(totalSamples * 2) // 16-bit = 2 byte/sample
        var offset       = 0

        isRecording = true
        recorder.startRecording()

        val buffer = ByteArray(bufferSize)
        var second = 0

        while (isRecording && offset < audioData.size) {
            val read = recorder.read(buffer, 0, buffer.size)
            if (read > 0) {
                val copyLen = minOf(read, audioData.size - offset)
                buffer.copyInto(audioData, offset, 0, copyLen)
                offset += copyLen

                // Her saniye progress güncelle
                val newSecond = offset / (SAMPLE_RATE * 2)
                if (newSecond > second) {
                    second = newSecond
                    val remaining = RECORD_DURATION_SEC - second
                    onProgress("🎙️ Dinleniyor… ($remaining sn)")
                }
            }
        }

        recorder.stop()
        recorder.release()
        isRecording = false

        return if (offset > 0) audioData.copyOf(offset) else null
    }

    fun stopRecording() {
        isRecording = false
    }

    private fun sendToAcrCloud(pcmData: ByteArray): RecognitionResult {
        try {
            val host      = BuildConfig.ACR_HOST
            val accessKey = BuildConfig.ACR_ACCESS_KEY
            val secretKey = BuildConfig.ACR_SECRET_KEY

            val timestamp  = (System.currentTimeMillis() / 1000).toString()
            val dataType   = "audio"
            val signatureVersion = "1"
            val httpMethod = "POST"
            val httpUri    = "/v1/identify"

            // HMAC-SHA1 imzası
            val signStr = "$httpMethod\n$httpUri\n$accessKey\n$dataType\n$signatureVersion\n$timestamp"
            val signature = hmacSha1(signStr, secretKey)

            // Base64 PCM
            val audioBase64 = Base64.encodeToString(pcmData, Base64.NO_WRAP)

            val body = FormBody.Builder()
                .add("sample",            audioBase64)
                .add("sample_bytes",      pcmData.size.toString())
                .add("access_key",        accessKey)
                .add("data_type",         dataType)
                .add("signature_version", signatureVersion)
                .add("signature",         signature)
                .add("timestamp",         timestamp)
                .build()

            val request = Request.Builder()
                .url("https://$host/v1/identify")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val json     = JSONObject(response.body?.string() ?: "{}")

            Log.d(TAG, "ACRCloud yanıtı: $json")

            val status = json.optJSONObject("status")
            val code   = status?.optInt("code", -1) ?: -1

            return when (code) {
                0 -> {
                    // Başarılı tanıma
                    val music  = json.optJSONObject("metadata")
                        ?.optJSONArray("music")
                        ?.optJSONObject(0)

                    val title  = music?.optString("title", "") ?: ""
                    val artist = music?.optJSONArray("artists")
                        ?.optJSONObject(0)
                        ?.optString("name", "") ?: ""
                    val album  = music?.optJSONObject("album")
                        ?.optString("name", "") ?: ""

                    RecognitionResult(title = title, artist = artist,
                        album = album, success = true)
                }
                1001 -> RecognitionResult(
                    title = "", artist = "", success = false,
                    errorMsg = "Şarkı tanınamadı. Müziğin duyulduğu bir yerde deneyin."
                )
                else -> RecognitionResult(
                    title = "", artist = "", success = false,
                    errorMsg = status?.optString("msg", "Bilinmeyen hata") ?: "Hata kodu: $code"
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "ACRCloud isteği başarısız: ${e.message}")
            return RecognitionResult(
                title = "", artist = "", success = false,
                errorMsg = "Bağlantı hatası: ${e.message}"
            )
        }
    }

    private fun hmacSha1(data: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA1"))
        return Base64.encodeToString(mac.doFinal(data.toByteArray()), Base64.NO_WRAP)
    }
}
