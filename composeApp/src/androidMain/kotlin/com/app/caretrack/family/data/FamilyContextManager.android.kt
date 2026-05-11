package com.app.caretrack.family.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.app.caretrack.auth.model.Role
import com.app.caretrack.common.mock.MockData
import com.app.caretrack.family.model.FamilyModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.familyDataStore: DataStore<Preferences> by preferencesDataStore(name = "family_prefs")

class FamilyContextManagerImpl(private val context: Context) : FamilyContext {

    private val store: DataStore<Preferences> = context.familyDataStore

    private val _selectedFamilyId = MutableStateFlow<Long?>(runBlocking {
        store.data.first()[KEY_SELECTED_FAMILY_ID]
    })
    override val selectedFamilyId: StateFlow<Long?> = _selectedFamilyId.asStateFlow()

    private val _selectedFamily = MutableStateFlow<FamilyModel?>(null)
    override val selectedFamily: StateFlow<FamilyModel?> = _selectedFamily.asStateFlow()

    private val _userFamilies = MutableStateFlow<List<FamilyModel>>(emptyList())
    override val userFamilies: StateFlow<List<FamilyModel>> = _userFamilies.asStateFlow()

    private val _activeRole = MutableStateFlow<Role?>(null)
    override val activeRole: StateFlow<Role?> = _activeRole.asStateFlow()

    companion object {
        private val KEY_SELECTED_FAMILY_ID = longPreferencesKey("selected_family_id")
    }

    override fun selectFamily(familyId: Long) {
        // Actualizar estados en memoria sincrónicamente → la UI reacciona de inmediato
        _selectedFamilyId.value = familyId
        _selectedFamily.value = MockData.families.find { it.id == familyId }
        _activeRole.value = MockData.familyRole(familyId)
        // Persistir en DataStore de forma asíncrona (solo para restaurar entre sesiones)
        CoroutineScope(Dispatchers.IO).launch {
            store.edit { prefs -> prefs[KEY_SELECTED_FAMILY_ID] = familyId }
        }
    }

    override fun loadUserFamilies() {
        _userFamilies.value = MockData.families

        val selectedId = _selectedFamilyId.value
        if (selectedId != null) {
            _selectedFamily.value = MockData.families.find { it.id == selectedId }
            _activeRole.value = MockData.familyRole(selectedId)
        }
        // Si solo hay una familia, seleccionarla automáticamente
        else if (MockData.families.size == 1) {
            selectFamily(MockData.families.first().id)
        }
    }

    override fun clearSelection() {
        CoroutineScope(Dispatchers.IO).launch {
            store.edit { it.remove(KEY_SELECTED_FAMILY_ID) }
            _selectedFamilyId.value = null
            _selectedFamily.value = null
            _activeRole.value = null
        }
    }
}

actual fun createFamilyContextManager(context: Any?): FamilyContext {
    return FamilyContextManagerImpl(context as Context)
}
