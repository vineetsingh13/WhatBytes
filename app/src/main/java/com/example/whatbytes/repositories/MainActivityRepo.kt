package com.example.whatbytes.repositories

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.whatbytes.Models.contactModel
import com.example.whatbytes.viewModels.MainActivityViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar

object MainActivityRepo {

    fun recentContacts(): MutableLiveData<ArrayList<contactModel>>{
        val contact=MutableLiveData<ArrayList<contactModel>>()
        val data=arrayListOf<contactModel>()

        val db = Firebase.firestore
        val calendar = Calendar.getInstance()


        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time


        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.time


        val startTimestamp = Timestamp(startOfDay)
        val endTimestamp = Timestamp(endOfDay)


        db.collection("contacts")
            .whereGreaterThanOrEqualTo("dateAdded", startTimestamp)
            .whereLessThanOrEqualTo("dateAdded", endTimestamp)
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {

                    for (document in documents) {
                        val name = document.getString("name")
                        val phone = document.getString("phone")

                        val c=contactModel(name.toString(), phone.toString())
                        data.add(c)

                        Log.d(TAG, "Contact: $name, Phone: $phone")

                    }
                    contact.value=data
                    Log.d(TAG, "data: $data")
                } else {
                    Log.d(TAG, "No contacts added today")
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting documents", e)
            }


        return contact
    }
}