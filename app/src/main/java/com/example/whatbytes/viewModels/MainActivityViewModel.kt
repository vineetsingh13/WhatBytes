package com.example.whatbytes.viewModels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.whatbytes.Models.contactModel
import com.example.whatbytes.repositories.MainActivityRepo

class MainActivityViewModel:ViewModel() {

    val contactProgress = MutableLiveData<Int>()
    fun recentContact(): MutableLiveData<ArrayList<contactModel>>{
        var contact=MutableLiveData<ArrayList<contactModel>>()
        contact= MainActivityRepo.recentContacts()
        return contact
    }

}