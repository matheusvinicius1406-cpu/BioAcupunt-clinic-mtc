package com.bioacupunt.prontuario.presentation

import androidx.lifecycle.ViewModelProvider
import com.bioacupunt.prontuario.domain.usecase.ProntuarioUseCases

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
            updateEntryUC = cases.updateEntry,
            deleteEntry = cases.deleteEntry
        ) as T
    }
}
