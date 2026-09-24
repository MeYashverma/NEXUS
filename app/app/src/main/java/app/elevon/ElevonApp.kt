package app.elevon

import android.app.Application

class ElevonApp : Application() {

    lateinit var session: ElevonSession
        private set

    override fun onCreate() {
        super.onCreate()
        session = ElevonSession(this)
        session.hid.start()
    }
}
