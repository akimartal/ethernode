package com.dataart.aakimov.ethernode.bl

import android.os.Handler
import android.os.HandlerThread
import com.dataart.aakimov.ethernode.Constants
import org.ethereum.geth.Geth
import org.ethereum.geth.Header

class RemoteClient(keystorePath: String, passphrase: String) : Client(keystorePath, passphrase) {
    private lateinit var handler: Handler
    private val handlerThread = HandlerThread("HandlerThreadOfRemoteNode")

    init {
        client = Geth.newEthereumClient(Constants.infurApi)
    }

    override fun startWithSubscription(onNewHeadAction: (Header) -> Unit)
            : HeadersSubscription {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        handler.apply {
            val time = 5000L
            val runnable = object : Runnable {
                override fun run() {
                    onNewHeadAction(client.getHeaderByNumber(context, -1))
                    postDelayed(this, time)
                }
            }
            post(runnable)
        }
        return object : HeadersSubscription {
            override fun unsubscribe() {
                stop()
            }
        }
    }

    override fun stop() {
        handlerThread.quit()
    }
}