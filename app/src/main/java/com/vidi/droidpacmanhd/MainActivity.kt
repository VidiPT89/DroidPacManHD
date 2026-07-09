package com.vidi.droidpacmanhd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.vidi.droidpacmanhd.game.GameEngine
import com.vidi.droidpacmanhd.haptics.Haptics
import com.vidi.droidpacmanhd.i18n.Loc
import com.vidi.droidpacmanhd.sound.SoundEngine
import com.vidi.droidpacmanhd.ui.GameScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Loc.init(applicationContext)
        SoundEngine.init(applicationContext)
        Haptics.init(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val engine = remember { GameEngine(applicationContext) }
                    GameScreen(engine)
                }
            }
        }
    }
}
