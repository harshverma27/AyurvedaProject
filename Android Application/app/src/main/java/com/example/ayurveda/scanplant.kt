package com.example.ayurveda


import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okio.IOException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
@Composable
fun ScanPlant() {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var resultText by remember { mutableStateOf("No plant identified") }
    val api = stringResource(id = com.example.ayurveda.R.string.api)
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    // Trigger image processing when imageUri updates
    LaunchedEffect(imageUri) {
        imageUri?.let { uri ->
            processImage(api, uri, context) { result ->
                resultText = result
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(50.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Scan Plant", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(25.dp))

        Button(onClick = { imagePicker.launch("image/*") }) {
            Text(text = "Upload Image")
        }


        imageUri?.let {
            Text(text = "Selected Image: ${it.lastPathSegment}")
        }

        Spacer(modifier = Modifier.height(20.dp))

        ShowInfo(resultText)
    }
}


fun processImage( api: String,uri: Uri, context: Context, onResult: (String) -> Unit) {
    val file = uriToFile(uri, context) ?: return

    // Use a coroutine to perform the network request
    CoroutineScope(Dispatchers.IO).launch {
        val result = identifyPlant(file, api)
        withContext(Dispatchers.Main) {
            onResult(result)
        }
    }
}

private fun uriToFile(uri: Uri, context: Context): File? {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
    val file = File(context.cacheDir, "temp_image.jpg")
    FileOutputStream(file).use { output -> inputStream.copyTo(output) }
    return file
}
suspend fun identifyPlant(imageFile: File, api: String): String {
    return withContext(Dispatchers.IO) {
        val apiKey = api  // Replace with your API key
        val apiUrl = "https://my-api.plantnet.org/v2/identify/all?api-key=$apiKey"

        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("organs", "leaf")
            .addFormDataPart(
                "images", imageFile.name,
                imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext "Error: ${response.message}"
            }

            val jsonResponse = JSONObject(response.body?.string() ?: "{}")
            val bestMatch = jsonResponse.optString("bestMatch", "Unknown")

            val commonNames = jsonResponse.optJSONArray("results")
                ?.optJSONObject(0)
                ?.optJSONObject("species")
                ?.optJSONArray("commonNames")

            val commonName = commonNames?.optString(0) ?: "Unknown"

            return@withContext "$bestMatch"
        } catch (e: Exception) {
            return@withContext "Error: ${e.message}"
        }
    }
}

@Composable
fun ShowInfo(resultText: String) {
    Card(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = resultText, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        }
    }

}