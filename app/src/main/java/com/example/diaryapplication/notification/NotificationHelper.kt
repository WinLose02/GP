package com.example.diaryapplication.notification

import android.app.PendingIntent
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Calendar

object NotificationHelper {

    // 알림 설정 (매일 특정 시간에 반복)
    fun scheduleDailyAlarm(context: Context, hour: Int, minute: Int) {

        context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("alarm_hour", hour)
            .putInt("alarm_minute", minute)
            .apply()

        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val intent = Intent(context, DiaryAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 오늘 설정한 시간을 계산
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // 이미 지난 시간이면 내일로 설정
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH,1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if(alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    // 알림 취소
    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, DiaryAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}