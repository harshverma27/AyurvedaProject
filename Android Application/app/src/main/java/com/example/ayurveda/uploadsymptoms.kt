package com.example.ayurveda

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

import org.json.JSONObject

data class HerbalResponse(
    @SerializedName("herbal_plants")
    val herbalPlants: List<HerbalPlant>
)

data class HerbalPlant(
    val name: String,

    @SerializedName("uses")  // Maps "uses" from JSON to "usage" in Kotlin
    val usage: String?
)
class SymptomViewModel : ViewModel() {
    var symptom = mutableStateOf("")
    var herbalPlants = mutableStateOf<List<HerbalPlant>>(emptyList())
    var errorMessage = mutableStateOf<String?>(null)

    fun updateSymptom(newSymptom: String) {
        symptom.value = newSymptom
    }

    fun updateHerbalPlants(plants: List<HerbalPlant>) {
        herbalPlants.value = plants
    }

    fun setError(msg: String?) {
        errorMessage.value = msg
    }

    fun reset() {
        symptom.value = ""
        herbalPlants.value = emptyList()
        errorMessage.value = null
    }
}
@SuppressLint("MissingPermission")
@Composable

fun SymptomInputScreen(
    navController: NavController,
    context: Context = LocalContext.current,
    viewModel: SymptomViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var localSymptom by remember { mutableStateOf(viewModel.symptom.value) }
    val herbalPlants by remember { viewModel.herbalPlants }
    val errorMessage by remember { viewModel.errorMessage }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Enter Your Symptoms", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = localSymptom,
            onValueChange = {
                localSymptom = it
                viewModel.updateSymptom(it)
            },
            label = { Text("Symptoms") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude

                    val prompt = """
                        Can you give me five herbal plants along with how to use them in a raw text form of a json object where both names and how to use are stored for specific symptom $localSymptom at lat $lat and long $lon, just give me the json thing no need to give introductory line also specify location in uses and give uses in 100 words
                    """.trimIndent()

                    getChatbotResponse(prompt) { responseJson ->
                        val gson = Gson()
                        val cleanedJson = responseJson
                            .removePrefix("\"```json\\n")
                            .removeSuffix("\\n```\"")
                            .replace("\\n", "")
                            .replace("\\\"", "\"")

                        Log.d("API_RESPONSE", "Cleaned JSON: $cleanedJson") // Log cleaned JSON
                        try {
                            val type = object : TypeToken<HerbalResponse>() {}.type
                            val response = gson.fromJson<HerbalResponse>(cleanedJson, type)
                            Log.d("JSON_DEBUG", "Parsed response: ${response.herbalPlants}")
                            viewModel.updateHerbalPlants(response.herbalPlants)
                            viewModel.setError(null)
                        } catch (e: Exception) {
                            Log.e("JSON_ERROR", "Parsing failed", e)
                            viewModel.setError("Failed to parse herbal recommendations.")
                        }
                    }
                } else {
                    Toast.makeText(context, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        }) {
            Text("Get Herbal Recommendations")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (herbalPlants.isNotEmpty()) {
            LazyColumn {
                items(herbalPlants) { plant ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .clickable {
                                val encodedName = Uri.encode(plant.name)  // Encode the name properly
                                val encodedUsage = Uri.encode(plant.usage?.replace("/", "%2F")) ?: ""// Encode usage & handle slashes

                                navController.navigate("plant_detail/$encodedName/$encodedUsage")
                            }
                    ) {
                        Text(
                            text = plant.name,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 18.sp
                        )
                    }
                }
            }
        } else if (errorMessage == null) {
            Text("No recommendations yet. Enter symptoms and tap the button.")
        }
    }
}
@Composable
fun PlantDetailScreen(plantName: String, plantUse: String) {
    var isBookmarked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = plantName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = {
                isBookmarked = !isBookmarked
                // TODO: Save to Firebase here
            }) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Bookmark"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "How to use:",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = plantUse,
            fontSize = 16.sp,
            lineHeight = 22.sp
        )
    }
}
fun getChatbotResponse(message: String, onResult: (String) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        val url = "http://172.20.10.4:8000/chat"
        val json = JSONObject().apply {
            put("message", message)
        }

        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)

        val request = Request.Builder().url(url).post(body).build()

        try {
            client.newCall(request).execute().use { response ->
                val rawResponse = response.body?.string() ?: "Error: Empty response"

                Log.d("API_RESPONSE", "Raw response: $rawResponse") // ✅ Only logs raw response

                withContext(Dispatchers.Main) {
                    onResult(rawResponse) // ✅ Returns raw response as String
                }
            }
        } catch (e: Exception) {
            Log.e("API_ERROR", "Error fetching herbal recommendations: ${e.message}")
            withContext(Dispatchers.Main) {
                onResult("Error: ${e.message}")
            }
        }
    }
}
