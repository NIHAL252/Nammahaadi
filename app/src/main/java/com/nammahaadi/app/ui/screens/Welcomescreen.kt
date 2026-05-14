package com.nammahaadi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nammahaadi.app.utils.UserPreferences

@Composable
fun WelcomeScreen(onNameSaved: () -> Unit) {
    val context = LocalContext.current
    var name    by remember { mutableStateOf("") }
    var error   by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape     = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors    = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier            = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App icon + title
                Text(text = "🗺️", fontSize = 56.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = "ನಮ್ಮ ಹಾದಿ",
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF1976D2)
                )
                Text(
                    text  = "Namma Haadi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text      = "Community Path Guide for Rural Karnataka",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))
                HorizontalDivider()
                Spacer(Modifier.height(20.dp))

                Text(
                    text       = "Enter your name to get started",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text  = "Your name will appear on the leaderboard\nwhen you save paths or report conditions",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(16.dp))

                // Name input
                OutlinedTextField(
                    value         = name,
                    onValueChange = {
                        name  = it
                        error = false
                    },
                    label         = { Text("Your Name") },
                    placeholder   = { Text("e.g. Raju, Meena, Suresh...") },
                    singleLine    = true,
                    isError       = error,
                    supportingText = if (error) {
                        { Text("Please enter your name", color = Color.Red) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(20.dp))

                // Start button
                Button(
                    onClick = {
                        if (name.trim().length < 2) {
                            error = true
                        } else {
                            // Save name + generate userId locally
                            UserPreferences.saveUser(context, name.trim())
                            onNameSaved()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Text(
                        text       = "Start Mapping →",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text  = "No account needed · Works offline · Free",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}