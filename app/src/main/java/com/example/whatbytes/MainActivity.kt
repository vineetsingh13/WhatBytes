package com.example.whatbytes

import android.content.ContentProviderOperation
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.whatbytes.databinding.ActivityMainBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        

        val db = Firebase.firestore
//        val contact= hashMapOf(
//            "name" to "John Doe",
//            "phone" to "1234567890",
//        )
//
//        db.collection("contacts")
//            .add(contact)
//            .addOnSuccessListener { doc->
//                Log.d(TAG, "DocumentSnapshot added with ID: ${doc.id}")
//            }
//            .addOnFailureListener { e->
//                Log.w(TAG, "Error adding document", e)
//            }

        binding.btnSyncContacts.setOnClickListener {
            val currentDate = SimpleDateFormat("dd-MMMM-yyyy", Locale.getDefault()).format(Date())

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
                            Log.d(TAG, "Contact: $name, Phone: $phone")
                            // Add contact to list, update UI, etc.
                            saveContact(this, name.toString(), phone.toString())
                        }
                    } else {
                        Log.d(TAG, "No contacts added today")
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error getting documents", e)
                }
        }
    }

    fun saveContact(context: Context, name: String, phone: String) {
        val ops = ArrayList<ContentProviderOperation>()

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )

        // Name
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build()
        )

        // Phone Number
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )

        try {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Log.d("Contacts", "Contact added: $name, $phone")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Contacts", "Error adding contact: $name", e)
        }
    }
}