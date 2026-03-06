package com.example.carguardian

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            // 启动MainActivity
            val launchIntent = Intent(context, MainActivity::class.java)
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            
            // 添加FLAG_ACTIVITY_CLEAR_TOP确保只有一个实例
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            
            context.startActivity(launchIntent)
        }
    }
}
