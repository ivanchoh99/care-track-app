package com.app.caretrack.family.data

import com.app.caretrack.auth.model.Role
import com.app.caretrack.family.model.FamilyModel
import com.app.caretrack.family.model.FamilyPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FamilyContextManagerIOS : FamilyContext {
    
    private val _selectedFamilyId = MutableStateFlow<Long?>(null)
    override val selectedFamilyId: StateFlow<Long?> = _selectedFamilyId.asStateFlow()
    
    private val _selectedFamily = MutableStateFlow<FamilyModel?>(null)
    override val selectedFamily: StateFlow<FamilyModel?> = _selectedFamily.asStateFlow()
    
    private val _userFamilies = MutableStateFlow<List<FamilyModel>>(emptyList())
    override val userFamilies: StateFlow<List<FamilyModel>> = _userFamilies.asStateFlow()
    
    private val _activeRole = MutableStateFlow<Role?>(null)
    override val activeRole: StateFlow<Role?> = _activeRole.asStateFlow()
    
    override fun selectFamily(familyId: Long) {
        _selectedFamilyId.value = familyId
        val family = _userFamilies.value.find { it.id == familyId }
        _selectedFamily.value = family
        _activeRole.value = Role.FAMILY_ADMIN
    }
    
    override fun loadUserFamilies() {
        _userFamilies.value = listOf(
            FamilyModel(1, "uuid-1", "Familia Rodriguez", FamilyPlan.PREMIUM, true),
            FamilyModel(2, "uuid-2", "Familia García", FamilyPlan.BASIC, true)
        )
        
        val selectedId = _selectedFamilyId.value
        if (selectedId != null) {
            _selectedFamily.value = _userFamilies.value.find { it.id == selectedId }
            _activeRole.value = Role.FAMILY_ADMIN
        } else if (_userFamilies.value.size == 1) {
            selectFamily(_userFamilies.value.first().id)
        }
    }
    
    override fun clearSelection() {
        _selectedFamilyId.value = null
        _selectedFamily.value = null
        _activeRole.value = null
    }
}

actual fun createFamilyContextManager(context: Any?): FamilyContext = FamilyContextManagerIOS()