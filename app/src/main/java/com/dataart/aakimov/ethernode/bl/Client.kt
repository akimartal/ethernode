package com.dataart.aakimov.ethernode.bl

import org.ethereum.geth.*


abstract class Client(keystorePath: String, private val passphrase: String) {

    private val keystore = KeyStore(keystorePath, Geth.LightScryptN, Geth.LightScryptP)
    protected lateinit var client: EthereumClient
    protected val context = Context()

    abstract fun startWithSubscription(onNewHeadAction: (Header) -> Unit): HeadersSubscription
    abstract fun stop()

    fun getAccount(): Account {
        if (keystore.accounts.size() == 0L) {
            keystore.newAccount(passphrase)
        }
        return keystore.accounts[0]
    }

    fun getBalance(address: Address, blockNum: Long): BigInt = client.getBalanceAt(context,
            address, blockNum)

    fun sendEther(toAddress: Address, networkId: Long,
                  passphrase: String) {
        val fromAccount = getAccount()
        val defaultGasAmount = 90000L
        val amountToPass = 100000000000000
        val amount = BigInt(amountToPass)
        val nonce = client.getPendingNonceAt(context, fromAccount.address)
        val gas = Geth.newBigInt(defaultGasAmount)
        val gasPrice = client.suggestGasPrice(context)
        val data = byteArrayOf()
        var transaction = Geth.newTransaction(nonce, toAddress, amount, gas.int64, gasPrice, data)
        transaction = keystore.signTxPassphrase(fromAccount, passphrase, transaction,
                BigInt(networkId))
        client.sendTransaction(context, transaction)
    }

    fun getTokenBalance(contractAddress: String, contractAbi: String): String {
        val boundContract = Geth.bindContract(Geth.newAddressFromHex(contractAddress), contractAbi,
                client)
        val account = getAccount()

        val paramAddress = Geth.newInterface()
        paramAddress.address = account.address
        val params = Geth.newInterfaces(1)
        params.set(0, paramAddress)

        val balanceResult = Geth.newInterface()
        balanceResult.setDefaultBigInt()
        val results = Geth.newInterfaces(1)
        results.set(0, balanceResult)

        val opts = Geth.newCallOpts()
        opts.setContext(context)
        boundContract.call(opts, results, "balanceOf", params)
        val balance = balanceResult.bigInt.string()

        val symbolResult = getTokenSymbol(boundContract)
        return "$balance ${symbolResult.string}"
    }

    private fun getTokenSymbol(boundContract: BoundContract): Interface {
        val symbolResult = Geth.newInterface()
        symbolResult.setDefaultString()
        val results = Geth.newInterfaces(1)
        results.set(0, symbolResult)

        val opts = Geth.newCallOpts()
        opts.setContext(context)
        boundContract.call(opts, results, "symbol", Geth.newInterfaces(0))
        return symbolResult
    }

    interface HeadersSubscription {
        fun unsubscribe()
    }
}