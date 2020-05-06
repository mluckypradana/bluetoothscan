package com.example.bluetoothscan

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class MainActivity : AppCompatActivity() {
    private lateinit var viewAdapter: MyAdapter
    private lateinit var viewManager: LinearLayoutManager
    private lateinit var arrayOfFoundBTDevices: MutableList<BluetoothObject>
    private val LOG_TAG // Just for logging purposes. Could be anything. Set to app_name
            : String? = null
    private val REQUEST_ENABLE_BT = 99 // Any positive integer should work.

    private var mBluetoothAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        enableBluetoothOnDevice()
    }

    private fun enableBluetoothOnDevice() {
        if (mBluetoothAdapter == null) {
            Log.e(LOG_TAG, "This device does not have a bluetooth adapter")
            finish()
            // If the android device does not have bluetooth, just return and get out.
            // There's nothing the app can do in this case. Closing app.
        }

        // Check to see if bluetooth is enabled. Prompt to enable it
        if (!mBluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        else{
            arrayOfFoundBTDevices = getArrayOfAlreadyPairedBluetoothDevices()
            setupRecyclerView()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == 0) {
                // If the resultCode is 0, the user selected "No" when prompt to
                // allow the app to enable bluetooth.
                // You may want to display a dialog explaining what would happen if
                // the user doesn't enable bluetooth.
                Toast.makeText(this, "The user decided to deny bluetooth access", Toast.LENGTH_LONG)
                    .show()
            } else {
                Log.i(LOG_TAG, "User allowed bluetooth access!")
                arrayOfFoundBTDevices = getArrayOfAlreadyPairedBluetoothDevices()
                setupRecyclerView()
            }
        }
    }

    private fun setupRecyclerView() {
        viewManager = LinearLayoutManager(this)
        viewAdapter = MyAdapter(arrayOfFoundBTDevices)

        var recyclerView = findViewById<RecyclerView>(R.id.my_recycler_view).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter

        }
    }

    override fun onPause() {
        super.onPause()
        mBluetoothAdapter!!.cancelDiscovery()
    }

    private fun displayListOfFoundDevices() {
        arrayOfFoundBTDevices = ArrayList<BluetoothObject>()

        // start looking for bluetooth devices
        mBluetoothAdapter!!.startDiscovery()

        // Discover new devices
        // Create a BroadcastReceiver for ACTION_FOUND
        val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val action = intent.action
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND == action) {
                    // Get the bluetoothDevice object from the Intent
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    // Get the "RSSI" to get the signal strength as integer,
                    // but should be displayed in "dBm" units
                    val rssi = intent.getShortExtra(
                        BluetoothDevice.EXTRA_RSSI,
                        Short.MIN_VALUE
                    ).toInt()

                    // Create the device object and add it to the arrayList of devices
                    val bluetoothObject = createObject(device)
                    bluetoothObject?.let { arrayOfFoundBTDevices.add(it) }

//                    // 1. Pass context and data to the custom adapter
//                    val adapter = FoundBTDevicesAdapter(applicationContext, arrayOfFoundBTDevices)
//
//                    // 2. setListAdapter
//                    setListAdapter(adapter)
                }
            }
        }
        // Register the BroadcastReceiver
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mReceiver, filter)
    }

    private fun getArrayOfAlreadyPairedBluetoothDevices(): MutableList<BluetoothObject> {
        var arrayOfAlreadyPairedBTDevices: MutableList<BluetoothObject> = arrayListOf()

        // Query paired devices
        val pairedDevices: MutableSet<BluetoothDevice>? = mBluetoothAdapter!!.bondedDevices

        // Loop through paired devices
        if (pairedDevices != null) {
            for (device in pairedDevices) {
                // Create the device object and add it to the arrayList of devices
                arrayOfAlreadyPairedBTDevices.add(createObject(device))
            }
        }

        return arrayOfAlreadyPairedBTDevices
    }

    private fun createObject(device: BluetoothDevice): BluetoothObject {
        val bluetoothObject = BluetoothObject()
        bluetoothObject.name = device.name
        bluetoothObject.address = device.address
        bluetoothObject.state = device.bondState
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bluetoothObject.type = device.type
        } // requires API 18 or higher
        bluetoothObject.uuids = device.uuids
        return bluetoothObject
    }

}

class BluetoothObject {
    var uuids: Array<ParcelUuid>? = null
    var name: String? = null
    var address: String? = null
    var state: Int? = null
    var type: Int? = null
}
