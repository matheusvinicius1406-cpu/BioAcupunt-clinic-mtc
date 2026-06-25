package com.example.ui.screens.inteligencia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.intelligence.IntelligenceDao
import com.example.data.intelligence.TopicEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IntelligenceViewModel(private val dao: IntelligenceDao) : ViewModel() {

    val topics: StateFlow<List<TopicEntity>> = dao.getAllTopics()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTopic(topic: TopicEntity) {
        viewModelScope.launch {
            dao.insertTopic(topic)
        }
    }
}
