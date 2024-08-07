package com.example.whatbytes

import android.Manifest
import android.app.AlertDialog
import android.content.ContentProviderOperation
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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
    private val PERMISSION_REQ_CODE=100

    var permissionExplainationDialogShown=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        

        val db = Firebase.firestore


        binding.btnSyncContacts.setOnClickListener {

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

    override fun onStart() {
        super.onStart()
        requestPermission()
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

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show()

        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_CONTACTS),
                PERMISSION_REQ_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show()
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) && permissionExplainationDialogShown
            ) {
                showSettingsRedirectDialog()
            } else {
                if(!permissionExplainationDialogShown){
                    showPermissionExplanationDialog()
                    permissionExplainationDialogShown=true
                }
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Location Permission is needed for the app.")
        builder.setCancelable(false)
        builder.setPositiveButton("OK") { dialog, which ->
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQ_CODE
            )
        }

        val alertDialog = builder.create()

        alertDialog.show()
    }

    private fun showSettingsRedirectDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Contacts permission is needed in order for the app to work")
        builder.setCancelable(false)
        builder.setPositiveButton("OK") { dialog, which ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }
}