package com.mindalive.app

import android.content.Context
import androidx.work.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object GestorNotificaciones {

    fun programarAlarma(
        context: Context,
        alarmaId: Long,
        titulo: String,
        descripcion: String,
        tipo: String,
        fechaHora: LocalDateTime,
        repeticion: String
    ) {
        val ahora = LocalDateTime.now()
        val delay = java.time.Duration.between(ahora, fechaHora).toMillis()

        if (delay <= 0) return

        val datos = workDataOf(
            "alarmaId" to alarmaId,
            "titulo" to titulo,
            "descripcion" to descripcion,
            "tipo" to tipo
        )

        val request = OneTimeWorkRequestBuilder<AlarmaWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(datos)
            .addTag("alarma_$alarmaId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "alarma_$alarmaId",
            ExistingWorkPolicy.REPLACE,
            request
        )

        // Si es repetición diaria o semanal programamos la siguiente también
        if (repeticion == "DIARIA") {
            programarAlarma(context, alarmaId, titulo, descripcion, tipo,
                fechaHora.plusDays(1), repeticion)
        } else if (repeticion == "SEMANAL") {
            programarAlarma(context, alarmaId, titulo, descripcion, tipo,
                fechaHora.plusWeeks(1), repeticion)
        }
    }

    fun cancelarAlarma(context: Context, alarmaId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("alarma_$alarmaId")
    }

    fun cancelarTodasLasAlarmas(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }
}