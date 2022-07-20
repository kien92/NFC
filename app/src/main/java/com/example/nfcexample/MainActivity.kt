package com.example.nfcexample

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.example.nfcexample.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var nFCAdapter: NfcAdapter;
    private lateinit var mNfcPendingIntent: PendingIntent
    private lateinit var mNdefExchangeFilters: Array<IntentFilter>
    private val TAG: String = MainActivity::class.java.toString()
    private val DATA: String = "KIEN"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        nFCAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nFCAdapter == null) {
            Log.i(TAG, "Nfc disable")
            return
        }
        initIntent()
    }

    private fun getNdefMessages(intent: Intent): ArrayList<NdefMessage> {
        var msgs: ArrayList<NdefMessage>? = null
        intent.action?.let {
            if (isNfcAdapter(it)){
              val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)!!.map { data -> data as Parcelable }.toTypedArray()
                if(rawMsgs!=null){
                   msgs = ArrayList<NdefMessage>()
                    rawMsgs.forEach { plan->
                        msgs!!.add(plan as NdefMessage)
                    }
                }else{
                    var emtyArray  : ByteArray = ByteArray(0)
                    val record = NdefRecord(NdefRecord.TNF_UNKNOWN, emtyArray, emtyArray, emtyArray)
                    val msg = NdefMessage(Array<NdefRecord>(0){record})
                    msgs = ArrayList<NdefMessage>()
                    msgs!!.add(msg)
                }
            }else{
                Log.d(TAG, "Unknown intent.");
            }
            return msgs!!
        }
        return arrayListOf()
    }

    private val isNfcAdapter: (act: String) -> Boolean = {
        NfcAdapter.ACTION_NDEF_DISCOVERED === it
    }

    private fun enableNdefExchangeMode() {
        if (nFCAdapter == null) {
            return
        }
        if (Build.VERSION.SDK_INT < 14) {
            nFCAdapter.enableForegroundNdefPush(this, getNoteAsNdef())
        } else {
            nFCAdapter.setNdefPushMessage(getNoteAsNdef(), this)
        }
        nFCAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mNdefExchangeFilters, null)
    }

    override fun onResume() {
        super.onResume()
        enableNdefExchangeMode()
    }

    private fun initIntent() {
        mNfcPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            0
        )
        var ndefDetected = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        try {
            ndefDetected.addDataType("text/plain")
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            e.printStackTrace()
        }
        mNdefExchangeFilters = Array<IntentFilter>(1) { ndefDetected }
    }

    private fun getNoteAsNdef(): NdefMessage {
        var textBytes = DATA.toByteArray()
        var textRecord = NdefRecord(
            NdefRecord.TNF_MIME_MEDIA, "text/plain".toByteArray(),
            byteArrayOf(), textBytes
        )
        return NdefMessage(Array<NdefRecord>(1) { textRecord })
    }


    override fun onNewIntent(intent: Intent) {
        // NDEF exchange mode
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val msgs: Array<NdefMessage> = getNdefMessages(intent).toTypedArray()
            binding.tvReceived.text = String(msgs[0].records[0].payload, Charsets.UTF_8)
        }
    }
}