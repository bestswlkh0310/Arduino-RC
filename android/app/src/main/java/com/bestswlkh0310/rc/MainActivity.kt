package com.bestswlkh0310.rc
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bestswlkh0310.rc.databinding.ActivityMainBinding
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val btAdapter: BluetoothAdapter by lazy  { BluetoothAdapter.getDefaultAdapter() }
    private lateinit var pairedDevices: Set<BluetoothDevice>
    private lateinit var btArrayAdapter: ArrayAdapter<String>
    private val deviceAddressArray: ArrayList<String> = arrayListOf()

    private val REQUEST_ENABLE_BT = 1
    private var btSocket: BluetoothSocket? = null
    private var connectedThread: ConnectedThread? = null

    @RequiresApi(Build.VERSION_CODES.S)
    private val permissionList = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
    )

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        ActivityCompat.requestPermissions(this, permissionList, 1)

        if (!btAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("TAG", "권한 에러! - onCreate() called")
                return
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        initAdapter()
        initController()
        initSearchBtn()
    }

    private fun initAdapter() {
        btArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        with(binding.listview) {
            adapter = btArrayAdapter
            onItemClickListener =
                AdapterView.OnItemClickListener { parent, view, position, id ->
                    itemClick(position)
                }
        }
    }

    private fun initSearchBtn() {
        binding.btnSearch.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.d("TAG", "search 권한 에러 - onClickButtonSearch() called")
                return@setOnClickListener
            }
            if (btAdapter.isDiscovering) {
                btAdapter.cancelDiscovery()
            } else {
                if (btAdapter.isEnabled) {
                    btAdapter.startDiscovery()
                    btArrayAdapter.clear()
                    deviceAddressArray.clear()
                    val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                    registerReceiver(receiver, filter)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initController() {
        with(binding) {
            btnLeft.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> write(RequestMsg.DOWN_L)
                    MotionEvent.ACTION_UP -> write(RequestMsg.UP_L)
                }
                true
            }
            btnRight.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> write(RequestMsg.DOWN_R)
                    MotionEvent.ACTION_UP -> write(RequestMsg.UP_R)
                }
                true
            }
            btnFront.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> write(RequestMsg.DOWN_F)
                    MotionEvent.ACTION_UP -> write(RequestMsg.UP_F)
                }
                true
            }
            btnBack.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> write(RequestMsg.DOWN_B)
                    MotionEvent.ACTION_UP -> write(RequestMsg.UP_B)
                }
                true
            }
        }
    }

    private fun write(msg: Int) {
        Log.d("TAG", "$msg - write() called")
        connectedThread?.write(msg.toString())
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                val deviceName = device?.name
                Log.d("TAG", "$deviceName - onReceive() called")
                val deviceHardwareAddress = device?.address
                if (deviceName == null) return
                Log.d("TAG", "$deviceHardwareAddress - onReceive() called")
                btArrayAdapter.add(deviceName)
                deviceAddressArray.add(deviceHardwareAddress!!)
                btArrayAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun itemClick(position: Int) {
        val name = btArrayAdapter.getItem(position).toString()
        val address = deviceAddressArray[position]
        val device = btAdapter.getRemoteDevice(address)
        var flag = true
        val setStatusText = { it: String -> binding.textStatus.text = it }

        showToast(name)
        setStatusText("연결 중..")

        try {
            btSocket = createBluetoothSocket(device)
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            btSocket?.connect()
        } catch (e: IOException) {
            Log.d("TAG", "${e.message} - onItemClick() called")
            flag = false
            setStatusText("연결 실패")
            e.printStackTrace()
        }

        if (flag) {
            setStatusText("연결 성공! - $name")
            connectedThread = ConnectedThread(btSocket!!)
            connectedThread?.start()
        }
    }

    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        return try {
            val m = device.javaClass.getMethod("createInsecureRfcommSocketToServiceRecord", UUID::class.java)
            m.invoke(device, BT_MODULE_UUID) as BluetoothSocket
        } catch (e: Exception) {
            Log.e("TAG", "Could not create Insecure RFComm Connection", e)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("TAG", "권한이 없어요 - createBluetoothSocket() called")
            }
            device.createRfcommSocketToServiceRecord(BT_MODULE_UUID)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

}
