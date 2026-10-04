package com.luauai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// Extensão singleton do DataStore para o Application
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "luauai_prefs")
