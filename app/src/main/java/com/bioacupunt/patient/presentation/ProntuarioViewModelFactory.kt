package com.bioacupunt.patient.presentation

import androidx.lifecycle.ViewModelProvider
import com.bioacupunt.prontuario.domain.usecase.ProntuarioUseCases
import com.bioacupunt.prontuario.presentation.ProntuarioViewModel

class ProntuarioViewModelFactory(
    private val cases: ProntuarioUseCases
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProntuarioViewModel(
            getProntuario = cases.getProntuario,
            saveProntuario = cases.saveProntuario,
            observeEntries = cases.observeEntries,
            addEntry = cases.addEntry,
            updateEntry = cases.updateEntry,
            deleteEntry = cases.deleteEntry
        ) as T
    }
}
