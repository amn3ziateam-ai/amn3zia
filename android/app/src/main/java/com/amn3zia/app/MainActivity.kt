package com.amn3zia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.amn3zia.app.core.privacy.ScreenProtection
import com.amn3zia.app.ui.nav.AmnNavGraph
import com.amn3zia.app.ui.theme.AmnTheme

class MainActivity : ComponentActivity() {

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            AmnApplication.from(applicationContext).autoClean.onAppForegrounded()
        }
        override fun onStop(owner: LifecycleOwner) {
            AmnApplication.from(applicationContext).autoClean.onAppClosed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Screen Protection: FLAG_SECURE blocks screenshots/recording and blurs
        // this activity in the recent-apps switcher (OS-level for FLAG_SECURE windows).
        ScreenProtection.applyTo(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)

        setContent {
            AmnApp()
        }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        super.onDestroy()
    }
}

@Composable
private fun AmnApp() {
    AmnTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AmnNavGraph()
        }
    }
}
