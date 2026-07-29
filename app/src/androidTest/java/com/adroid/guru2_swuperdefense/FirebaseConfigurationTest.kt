package com.adroid.guru2_swuperdefense

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseConfigurationTest {
    @Test
    fun defaultFirebaseAppUsesTeamProject() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = FirebaseApp.initializeApp(context)

        assertNotNull(app)
        assertEquals("swuper-defense-guru2", app?.options?.projectId)
        assertEquals("com.adroid.guru2_swuperdefense", context.packageName)
    }
}
