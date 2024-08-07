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
import android.os.Handler
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.whatbytes.databinding.ActivityMainBinding
import com.example.whatbytes.viewModels.MainActivityViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQ_CODE=100

    var permissionExplainationDialogShown=false

    lateinit var mainActivityViewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainActivityViewModel=ViewModelProvider(this).get(MainActivityViewModel::class.java)

        binding.btnSyncContacts.setOnClickListener {
            addContact()
        }
    }

    override fun onStart() {
        super.onStart()
        requestPermission()
    }

    fun addContact() {
        binding.progressBar.visibility=View.VISIBLE
        binding.btnSyncContacts.isActivated=false

        mainActivityViewModel.recentContact().observe(this, Observer { response ->
            response?.let {
                val totalContacts = it.size
                var progress = 0
                var delay=0L
                if(it.size<=10){
                    delay=500L
                }else if(it.size<=100){
                    delay=100L
                }else{
                    delay=10L
                }
                // Launch a coroutine on the Main (UI) thread
                CoroutineScope(Dispatchers.Main).launch {
                    for ((index, document) in it.withIndex()) {
                        delay(delay)

                        Log.d("DUMMY", "Contact: ${document.name}, Phone: ${document.phone}")
                        saveContact(this@MainActivity, document.name, document.phone)

                        // Update progress
                        progress = ((index + 1) * 100) / totalContacts
                        binding.progressBar.progress = progress
                    }

                    // Hide the progress bar when done
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "All contacts added!", Toast.LENGTH_SHORT).show()
                    binding.btnSyncContacts.isActivated=true
                }
            }
        })
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

            //Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show()

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