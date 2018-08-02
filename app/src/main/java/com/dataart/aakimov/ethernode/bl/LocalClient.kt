package com.dataart.aakimov.ethernode.bl

import org.ethereum.geth.*

class LocalClient(keystorePath: String, passphrase: String, nodeLocation: String,
                  networkId: Long)
    : Client(keystorePath, passphrase) {

    private var node: Node
    var ethSubscription: Subscription? = null

    init {
        val nodeConfig = NodeConfig()
        nodeConfig.ethereumNetworkID = networkId
        nodeConfig.ethereumGenesis = Geth.testnetGenesis()
        node = Geth.newNode(nodeLocation, nodeConfig)
    }

    override fun startWithSubscription(onNewHeadAction: (Header) -> Unit): HeadersSubscription {
        node.start()
        client = node.ethereumClient
        val handler = object : NewHeadHandler {
            override fun onError(error: String) {}
            override fun onNewHead(header: Header) {
                onNewHeadAction(header)
            }
        }
        ethSubscription = client.subscribeNewHead(context, handler, 16)
        return object : HeadersSubscription {
            override fun unsubscribe() {
                stop()
            }
        }
    }

    override fun stop() {
        node.stop()
    }
}