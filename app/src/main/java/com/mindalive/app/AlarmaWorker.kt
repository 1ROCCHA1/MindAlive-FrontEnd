package com.mindalive.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class AlarmaWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val titulo = inputData.getString("titulo") ?: "Recordatorio"
        val descripcion = inputData.getString("descripcion") ?: ""
        val tipo = inputData.getString("tipo") ?: "RECORDATORIO"
        val alarmaId = inputData.getLong("alarmaId", -1)

        val channelId = if (tipo == "MEDICAMENTO") "canal_medicamentos" else "canal_recordatorios"
        val channelNombre = if (tipo == "MEDICAMENTO") "Medicamentos" else "Recordatorios"
        val prioridad = if (tipo == "MEDICAMENTO")
            NotificationManager.IMPORTANCE_HIGH
        else
            NotificationManager.IMPORTANCE_DEFAULT

        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val canal = NotificationChannel(channelId, channelNombre, prioridad)
        notificationManager.createNotificationChannel(canal)

        val emoji = if (tipo == "MEDICAMENTO") "💊" else "📅"

        val notificacion = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$emoji $titulo")
            .setContentText(descripcion.ifEmpty { if (tipo == "MEDICAMENTO") "Es hora de tomar tu medicamento" else "Tienes un recordatorio pendiente" })
            .setPriority(if (tipo == "MEDICAMENTO") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alarmaId.toInt(), notificacion)

        return Result.success()
    }
}