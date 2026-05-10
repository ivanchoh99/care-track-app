package com.app.caretrack.family.data

import com.app.caretrack.auth.model.Role
import com.app.caretrack.family.model.FamilyModel
import kotlinx.coroutines.flow.StateFlow

interface FamilyContext {
    val selectedFamilyId: StateFlow<Long?>
    val selectedFamily: StateFlow<FamilyModel?>
    val userFamilies: StateFlow<List<FamilyModel>>
    val activeRole: StateFlow<Role?>
    
    fun selectFamily(familyId: Long)
    fun loadUserFamilies()
    fun clearSelection()
}

expect fun createFamilyContextManager(context: Any?): FamilyContext