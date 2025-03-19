package com.example.ayurveda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ayurveda.ui.theme.AyurvedaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AyurvedaTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(navController = navController, startDestination = "main_screen", modifier = Modifier.padding(innerPadding)) {
                        composable("main_screen") { MainScreen(navController) }
                        composable("scan_plant") { ScanPlant() }
                        composable("upload_symptoms") { UploadSymptoms() }
                        composable("bookmarked_plants") { BookmarkedPlants() }
                        composable("chat_bot") { ChatBot() }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(50.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Ayurveda App", fontSize = 30.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(50.dp))

        Button(modifier = Modifier.width(200.dp), onClick = {
            navController.navigate("scan_plant")
        }) {
            Text(text = "Scan A Plant")
        }

        Button(modifier = Modifier.width(200.dp), onClick = {
            navController.navigate("upload_symptoms")
        }) {
            Text(text = "Upload Your Symptoms")
        }

        Button(modifier = Modifier.width(200.dp), onClick = {
            navController.navigate("bookmarked_plants")
        }) {
            Text(text = "See Bookmarked Plants")
        }

        Button(modifier = Modifier.width(200.dp), onClick = {
            navController.navigate("chat_bot")
        }) {
            Text(text = "Open ChatBot")
        }
    }
}

