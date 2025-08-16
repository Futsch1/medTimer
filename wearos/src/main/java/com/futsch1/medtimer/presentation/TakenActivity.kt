/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.futsch1.medtimer.presentation

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.futsch1.medtimer.presentation.theme.MedTimerTheme
import com.futsch1.medtimer.shared.wear.TakenData
import kotlinx.coroutines.launch

class TakenActivity : ComponentActivity() {
    private lateinit var takenData: TakenData
    private lateinit var module: WatchWearModule

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearAppTaken("Android")
        }
        module = WatchWearModule(this);
        this.lifecycleScope.launch {

            val list = module.connectedNodes()
            Log.i("WEAR", "Connected nodes: $list")
            module.takenData()
                .collect { counterData -> work(counterData)
                //    Greeting(counterData.name)
                }
        }
    }
    fun vibrate(sleepTime: Long){
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(sleepTime, VibrationEffect.DEFAULT_AMPLITUDE) )
        }else{
            @Suppress("DEPRECATION")
            vib.vibrate(sleepTime)
        }
        Thread.sleep(sleepTime)
    }

    override fun onDestroy() {
        super.onDestroy()
        module.close()
    }
    fun work(takenData: TakenData) {
        vibrate(1000)
        this.takenData = takenData;
        var message="Android"
        if (takenData.active) {
            message = takenData.name
        }
        Log.i("WEAR", "Taken: ${message}")
        setContent {
            WearAppTaken(message)
        }
    }
    @Composable
    fun WearAppTaken(greetingName: String) {
        MedTimerTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Greeting(greetingName = greetingName)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Button(
                            onClick = {
                                Log.i("WEAR", "Taken ${takenData.reminderEventId}")
                                lifecycleScope.launch {
                                    module.recordMedicine(takenData.reminderEventId,"", takenData.notificationId )
                                }
                                finish()
                            },
                            content = {Text("Taken"    )},
                        )
                    }
                }
            }
        }
    }
    @Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
    @Composable
    fun DefaultPreviewTaken() {
        WearAppTaken("Insulina")
    }
}
