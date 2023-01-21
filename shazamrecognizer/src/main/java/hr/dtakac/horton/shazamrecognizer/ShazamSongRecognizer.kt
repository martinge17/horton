package hr.dtakac.horton.shazamrecognizer

import hr.dtakac.horton.domain.usecases.recognizesong.RecognizeSongResult
import hr.dtakac.horton.domain.recognizer.SongRecognizer
import hr.dtakac.horton.shazamrecognizer.fingerprint.NativeShazamFingerprintGenerator
import hr.dtakac.horton.shazamrecognizer.useragent
import java.time.Duration
import com.google.gson.Gson
import hr.dtakac.horton.shazamrecognizer.fingerprint.ShazamFingerprintResult
import hr.dtakac.horton.shazamrecognizer.useragent.Companion.USER_AGENTS
import kotlinx.coroutines.Dispatchers
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.*
import java.util.concurrent.TimeUnit

import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody

//TODO: DISPATCHER
//TODO: USERAGENTS


//TODO: IMPLEMENT SHAZAM RECON AND BACKEND COMMUNICATION. CHECK SONGREC CODE FOR REFERENCES
/*
    1. File to fingerprint
    2. If fingerprint success -> Build request body
    3. Send request and get response
 */

data class PostData(
    val geolocation: Geolocation,
    val signature: Signature,
    val timestamp: Long,
    val timezone: String
)

data class Geolocation(
    val altitude: Int,
    val latitude: Double,
    val longitude: Double
)

data class Signature( //That data comes from the Rust crosscompiled code. Check lib.rs for more info
    val samplems: Long,
    val timestamp: Long,
    val uri: String
)

class ShazamSongRecognizer : SongRecognizer {

    private val gson = Gson()
    private val dispatcherProvider =
    private val fingerprintGenerator = NativeShazamFingerprintGenerator(gson, dispatcherProvider)


    override val sampleDuration: Duration = Duration.ofSeconds(5L) //TODO: CHECK THIS OUT!!

    override suspend fun recognize(songFilePath: String): RecognizeSongResult{


        //Get fingerprint
        val fingerprintResult = fingerprintGenerator.getFingerprint(songFilePath)

        if (fingerprintResult is ShazamFingerprintResult.Success){ //Request

            //TODO


        }

        return RecognizeSongResult.NotRecognized //if not recognized

    }



    private fun buildShazamRequestBody(fingerprint: ShazamFingerprintResult.Success): RequestBody {
        val timestamp_ms = System.currentTimeMillis()

        //Geolocation could be random generated

        val postData = PostData(
            geolocation = Geolocation(altitude = 300, latitude = 45.0, longitude = 2.0),
            signature = Signature(fingerprint.data.sampleMs,timestamp_ms,fingerprint.data.uri),
            timestamp = timestamp_ms,
            timezone = "Europe/Paris"
        )

        val uuid1 = UUID.randomUUID().toString().uppercase()
        val uuid2 = UUID.randomUUID().toString()

        val url = "https://amp.shazam.com/discovery/v5/en/US/android/-/tag/$uuid1/$uuid2"

        val headers = Headers.Builder()
            .add("User-Agent", USER_AGENTS.random())
            .add("Content-Language", "en_US")
            .build()

        //Create client and build request
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .build()



        val request = Request.Builder()
            .url("$url?sync=true&webv3=true&sampling=true&connected=&shazamapiversion=v3&sharehub=true&video=v3")
            .headers(headers)
            .post(postData.toRequestBody("application/json; charset=utf-8".toMediaType())) //TODO: CHECK HOW TO CREATE BODY WITH OKHTTP3
            .build()
    }





}