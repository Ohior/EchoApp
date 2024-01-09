package com.recording.echo.Notifications


import android.os.Binder

class RecordingForegroundServiceBinder : Binder() {
    private var serviceInstance: RecordingForegroundService? = null

    fun setServiceInstance(service: RecordingForegroundService) {
        this.serviceInstance = service
    }

    fun getServiceInstance(): RecordingForegroundService? {
        return serviceInstance
    }
}