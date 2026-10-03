package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.screens.PhotoEditorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PhotoEditorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PhotoEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PhotoEditorScreen(viewModel = viewModel)
            }
        }
    }
}
