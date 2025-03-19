package com.example.ayurveda

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

// Retrofit API interface
interface PlantNetApi {
    @Multipart
    @POST("v2/identify")
    fun identifyPlant(
        @Part file: MultipartBody.Part,
        @Part("api-key") apiKey: RequestBody // ✅ Pass API key as RequestBody
    ): Call<PlantResponse>
}

// Data models
data class PlantResponse(
    val bestMatch: BestMatch?
)

data class BestMatch(
    val scientificName: String?,
    val commonNames: List<String>?
)

@Composable
fun ScanPlant() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var plantName by remember { mutableStateOf("") }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        uri?.let {
            uploadImage(it, context) { result ->
                plantName = result // Update UI with plant name
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(50.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Scan Plant", fontSize = 30.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(20.dp))

        imageUri?.let {
            Image(
                painter = rememberImagePainter(it),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .clickable { launcher.launch("image/*") }
            )
        } ?: Button(onClick = { launcher.launch("image/*") }) {
            Text(text = "Select Image")
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (plantName.isNotEmpty()) {
            Text(text = "Plant Name: $plantName", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Function to upload image
fun uploadImage(uri: Uri, context: Context, onResult: (String) -> Unit) {
    val apiKey = context.getString(R.string.plant_net_api_key) // ✅ Retrieve API key from secrets.xml
    Log.d("API_KEY", "Using API Key: $apiKey")

    val file = uriToFile(uri, context)
    if (file == null || !file.exists() || file.length() == 0L) {
        onResult("Error: Invalid image file")
        return
    }

    val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
    val body = MultipartBody.Part.createFormData("images", file.name, requestFile)
    val apiKeyBody = RequestBody.create("text/plain".toMediaTypeOrNull(), apiKey) // ✅ Convert API key to RequestBody

    // Retrofit setup with logging
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://my-api.plantnet.org/") // ✅ Ensure this is correct
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    val plantNetApi = retrofit.create(PlantNetApi::class.java)

    val call = plantNetApi.identifyPlant(body, apiKeyBody) // ✅ Corrected API call
    call.enqueue(object : Callback<PlantResponse> {
        override fun onResponse(call: Call<PlantResponse>, response: Response<PlantResponse>) {
            if (response.isSuccessful) {
                val bestMatch = response.body()?.bestMatch
                onResult(bestMatch?.scientificName ?: "Unknown plant")
            } else {
                onResult("Identification failed: ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<PlantResponse>, t: Throwable) {
            onResult("Error: ${t.message}")
        }
    })
}

// Function to convert Uri to File
fun uriToFile(uri: Uri, context: Context): File? {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
    tempFile.deleteOnExit()

    inputStream?.use { input ->
        FileOutputStream(tempFile).use { output ->
            input.copyTo(output)
        }
    }
    return tempFile
}