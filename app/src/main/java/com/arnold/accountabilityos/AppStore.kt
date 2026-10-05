package com.arnold.accountabilityos

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppStore(context:Context){
    private val prefs=context.getSharedPreferences("accountability_os",Context.MODE_PRIVATE)
    private val json=Json{ignoreUnknownKeys=true;encodeDefaults=true}
    fun load():AppState=prefs.getString("state",null)?.let{runCatching{json.decodeFromString<AppState>(it)}.getOrNull()}?:AppState()
    fun save(state:AppState){\n        // Use a synchronous commit so a just-saved finance balance or lesson is\n        // definitely on disk before the user leaves/reopens the app.\n        prefs.edit().putString("state",json.encodeToString(state)).commit()\n    }
}
