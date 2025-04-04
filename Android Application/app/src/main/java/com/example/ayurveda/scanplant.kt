package com.example.ayurveda


import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
@Composable
fun ScanPlant() {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var scientificName by remember { mutableStateOf<String?>(null) }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val api = stringResource(id = com.example.ayurveda.R.string.api)

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Scan Plant", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { imagePicker.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(painter = painterResource(id = android.R.drawable.ic_menu_camera), contentDescription = "Upload")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upload Image", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        imageUri?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = "Uploaded Image",
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        LaunchedEffect(imageUri) {
            imageUri?.let { uri ->
                isLoading = true
                processImage(api, uri, context) { result ->
                    scientificName = result
                    isLoading = false
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            scientificName?.let {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Plant Identified:", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Text(it, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                    }
                }
            }
        }

        LaunchedEffect(scientificName) {
            scientificName?.let { name ->
                if (name.isNotEmpty()) {
                    aiResponse = "Fetching details..."
                    aiResponse = getAiResponse("Give me the common name and method to use $name herbally in less than 100 words.")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        aiResponse?.let {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 300.dp) // Ensure it's scrollable if content is long
                    .verticalScroll(rememberScrollState()) // Enable scrolling
                    .background(Color.LightGray) // Optional: Improve UI visibility
                    .padding(8.dp)
                    .clip(shape = RoundedCornerShape(12.dp))
            ) {
                Text(text = "AI Response:", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Text(
                    text = it,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Justify,
                    color = Color.Black
                )
            }
        }
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

suspend fun getAiResponse(message: String): String {
    return withContext(Dispatchers.IO) {  // Run network request on IO thread
        val url = "http://172.20.10.4:8000/chat"
        val json = JSONObject().apply {
            put("message", message)
        }

        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, json.toString())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.body?.string() ?: "Error: Empty response"
            } else {
                "Error: ${response.code}"
            }
        }
    }
}