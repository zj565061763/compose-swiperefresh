package com.sd.demo.compose_swiperefresh

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_swiperefresh.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Content(this)
                }
            }
        }
    }
}

@Composable
private fun Content(
    activity: Activity,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Button(
            onClick = { activity.startActivity(Intent(activity, SampleVerticalActivity::class.java)) }
        ) {
            Text(text = "Vertical")
        }

        Button(
            onClick = { activity.startActivity(Intent(activity, SampleHorizontalActivity::class.java)) }
        ) {
            Text(text = "Horizontal")
        }

        Button(
            onClick = { activity.startActivity(Intent(activity, SampleInvisibleModeActivity::class.java)) }
        ) {
            Text(text = "Invisible mode")
        }

        Button(
            onClick = { activity.startActivity(Intent(activity, SampleCustomStyleActivity::class.java)) }
        ) {
            Text(text = "Custom style")
        }
    }
}

fun logMsg(block: () -> String) {
    Log.i("FSwipeRefresh-demo", block())
}