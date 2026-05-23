package com.example

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.BrowserDatabase
import com.example.data.repository.BrowserRepository
import com.example.ui.BrowserViewModel
import com.example.ui.BrowserViewModelFactory
import com.example.ui.screens.MainBrowserScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val database = BrowserDatabase.getDatabase(applicationContext)
    val repository = BrowserRepository(
      database.tabDao(),
      database.bookmarkDao(),
      database.historyDao(),
      database.extensionDao(),
      database.workspaceDao()
    )
    val viewModelFactory = BrowserViewModelFactory(repository)

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val viewModel: BrowserViewModel = viewModel(factory = viewModelFactory)
          
          MainBrowserScreen(viewModel = viewModel)
        }
      }
    }
  }
}
