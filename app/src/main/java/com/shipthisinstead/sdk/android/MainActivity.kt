package com.shipthisinstead.sdk.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shipthisinstead.sdk.`public`.ShipThisInsteadConfig
import com.shipthisinstead.sdk.`public`.ShipThisInsteadSDK
import com.shipthisinstead.sdk.`public`.STIEnvironment

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ShipThisInsteadSDK.configure(
            config = ShipThisInsteadConfig(
                organizationSlug = "dewuday",
                projectSlug = "sti",
                environment = STIEnvironment.Development(baseUrl = "http://192.168.1.161:3002"),
                ssoSecret = "ES10e1Ug5wmegWdg0r3Z/z2MAnuCM13fSfE6KSF+VdM=",
                userId = "demo-user-001",
                userName = "Demo User",
                userEmail = "demo@example.com",
                isPaidCustomer = true,
            ),
            context = applicationContext,
        )

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    DemoScreen()
                }
            }
        }
    }
}

@Composable
fun DemoScreen() {
    var showFeedback by remember { mutableStateOf(false) }
    var submittedCount by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = "ShipThisInstead SDK",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Android Demo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(40.dp))

            BadgedBox(
                badge = {
                    if (submittedCount > 0) {
                        Badge { Text(submittedCount.toString()) }
                    }
                },
            ) {
                Button(
                    onClick = { showFeedback = true },
                ) {
                    Text("Open Feedback")
                }
            }

            if (submittedCount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Submitted $submittedCount feedback item${if (submittedCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    if (showFeedback) {
        ShipThisInsteadSDK.FeedbackSheet(
            onDismiss = { showFeedback = false },
            onFeedbackSubmitted = {
                submittedCount++
                showFeedback = false
            },
        )
    }
}
