package hr.dtakac.horton.shazamrecognizer

import com.google.gson.Gson
import hr.dtakac.horton.domain.entities.Art
import hr.dtakac.horton.domain.entities.RecognizedSong
import hr.dtakac.horton.domain.recognizer.SongRecognizer
import hr.dtakac.horton.domain.usecases.recognizesong.RecognizeSongResult
import hr.dtakac.horton.shazamrecognizer.fingerprint.NativeShazamFingerprintGenerator
import hr.dtakac.horton.shazamrecognizer.fingerprint.ShazamFingerprintResult
import hr.dtakac.horton.shazamrecognizer.useragent.Companion.USER_AGENTS
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

//TODO: DISPATCHER

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

data class ShazamJSON(
    val track: Track
)

data class Track(
    val images: Images
)

data class Images(
    val coverart: String,
    val coverarthq: String
)

class ShazamSongRecognizer : SongRecognizer {

    private val gson = Gson()
    private val dispatcherProvider = //TODO Figure out how to use the dispatcherProvider
    private val fingerprintGenerator = NativeShazamFingerprintGenerator(gson, dispatcherProvider)


    override val sampleDuration: Duration = Duration.ofSeconds(5L) //TODO: CHECK THIS OUT!!

    override suspend fun recognize(songFilePath: String): RecognizeSongResult{

        //Get fingerprint
        val fingerprintResult = fingerprintGenerator.getFingerprint(songFilePath)

        if (fingerprintResult is ShazamFingerprintResult.Success){ //Request

            //TODO Coroutine to call queryShazam()


            val result = "" //TODO

            val parsedResult = parseShazamJSONtoRecognizedSong(result)

            return RecognizeSongResult.Success(parsedResult)


        }

        return RecognizeSongResult.NotRecognized //if not recognized

    }



    private fun queryShazam(fingerprint: ShazamFingerprintResult.Success): String? { //Returns JSON from Shazam
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

        // convert postData to JSON
        val json = Gson().toJson(postData)

        val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        //Create client and build request
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .build()

        //Create and send post request
        val request = Request.Builder()
            .url(url)
            .post(body)
            .headers(headers)
            .build()

        //Execute request
        val response = client.newCall(request).execute()

        //Return response as json
        if (response.isSuccessful) {
            //Return as json
            return Gson().toJson(response.body)
        }

        throw RuntimeException("Shazam Request Error!")

    }

    //TODO: REWORK THE PARSER TO GET THE THUMBNAILS URL....
    private fun parseShazamJSONtoRecognizedSong(json: String): RecognizedSong {

        val response = gson.fromJson(json,RecognizedSong::class.java) //Deserialize json to RecognizedSong object

        val title = response.title
        val subtitle = response.subtitle
        val art = getCoverImage(json) //TODO: Get thumbnail from request
        val recognitionTimestamp = response.recognitionTimestamp
        val releaseDate = response.releaseDate

        return RecognizedSong(title,subtitle,art, recognitionTimestamp, releaseDate)

    }

    private fun getCoverImage(json: String): Art {

        val response = gson.fromJson(json,ShazamJSON::class.java)

        val cover = response.track.images.coverart
        val coverhq =response.track.images.coverarthq

        return Art(coverhq,cover)

    }





}