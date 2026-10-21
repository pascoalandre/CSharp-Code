package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.csharp.ui.CSharpIdeScreen
import com.example.csharp.ui.CSharpIdeViewModel
import com.example.ui.theme.IdeBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = IdeBackground
                ) {
                    val viewModel: CSharpIdeViewModel = viewModel()
                    CSharpIdeScreen(viewModel = viewModel)
                }
            }
        }
    }
}
