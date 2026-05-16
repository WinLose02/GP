package com.example.diaryapplication.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.diaryapplication.MainActivity
import com.example.diaryapplication.R

class DiaryAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 알림 채널 생성
        val channelId = "diary_reminder"
        val manager = context.getSystemService(NotificationManager::class.java)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "일기 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        // 알람 클릭 시 앱 실행
        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 생성
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("오늘 일기를 작성해보세요!")
            .setContentText("오늘 하루는 어떠셨나요?")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // 클릭 시 알림 자동 삭제
            .build()

        val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val hour = prefs.getInt("alarm_hour", 21)
        val minute = prefs.getInt("alarm_minute", 0)
        NotificationHelper.scheduleDailyAlarm(context, hour, minute)

        manager.notify(1001, notification)
    }
}