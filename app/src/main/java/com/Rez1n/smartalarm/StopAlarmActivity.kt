package com.Rez1n.smartalarm

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StopAlarmActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stop_alarm)

        findViewById<Button>(R.id.btnStopAlarm).setOnClickListener {
            AlarmPlayer.stop()
            finish()
        }
    }
}
