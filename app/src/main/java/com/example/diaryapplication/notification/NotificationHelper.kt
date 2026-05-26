package com.example.diaryapplication.notification

import android.app.PendingIntent
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Calendar

object NotificationHelper {


    /*
        <알림 등록 전체 흐름>
            1. 사용자가 알림 시간 설정
            2. scheduleDailyAlarm(context, hour, minute) 호출
            3. SharedPreferences에 시간 저장
            4. AlarmManager에 알람 등록
            5. 설정한 시각 도달
            6. DiaryAlarmReceiver.onReceive() 실행
            7. 알림 표시 + 다음 날 알람 재등록

        <알림 취소 흐름>
            1. 사용자가 알림 끄기
            2. cancelAlarm(context) 호출
            3. AlarmManager에서 알람 제거
     */

    // 알림 설정 (매일 특정 시간에 반복)
    fun scheduleDailyAlarm(context: Context, hour: Int, minute: Int) {

        context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("alarm_hour", hour) // 설정한 '시' 값
            .putInt("alarm_minute", minute) // 설정한 '분'값
            .apply() // 변경사항을 저장

        val alarmManager = context.getSystemService(AlarmManager::class.java) // 알람 설정 및 취소를 담당하는 AlarmManager

        val intent = Intent(context, DiaryAlarmReceiver::class.java) // 알람이 울릴때 실행될 대상 -> DiaryAlarmReceiver의 onReceiver()
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


        // 안드로이드 버전 별 알람 설정
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