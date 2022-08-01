package com.example.todo.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.todo.data.ToDoDatabase
import com.example.todo.data.models.ToDoData
import com.example.todo.data.repository.ToDoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ToDoViewModel(app: Application): AndroidViewModel(app) {

    private val todoDao = ToDoDatabase.getDatabase(app).todoDao()
    private val repository: ToDoRepository = ToDoRepository(todoDao)
    val getAllData: LiveData<List<ToDoData>> = repository.getAllData
    val sortedDataHigh: LiveData<List<ToDoData>> = repository.sortByHighPriority()
    val sortedDataLow: LiveData<List<ToDoData>> = repository.sortByLowPriority()

    fun insertData(toDoData: ToDoData) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertData(toDoData)
        }
    }

    fun updateData(toDoData: ToDoData) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateData(toDoData)
        }
    }

    fun deleteItem(toDoData: ToDoData) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteItem(toDoData)
        }
    }

    fun deleteAll() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAll()
        }
    }

    fun searchDatabase(searchQuery: String) = repository.searchDatabase(searchQuery)
}