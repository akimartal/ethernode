package com.dataart.aakimov.ethernode.bl

import android.app.Activity
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.ethereum.geth.*
import java.util.concurrent.Executors


class ClientManager(activity: Activity) {
    private val keystorePath = activity.filesDir.toString() + "/keystore"
    private val passphrase = "MySuperSecurePasswordEver"
    private val pathToLocalNode = activity.filesDir.toString() + "/.ethereum"
    private var networkId = 3L //ropsten
    private val scheduler = Schedulers.from(Executors.newScheduledThreadPool(1))
    private var currentClient: Client = RemoteClient(keystorePath, passphrase)

    fun switch(isRemote: Boolean): Observable<Boolean> {
        return Observable.fromCallable {
            try {
                currentClient.stop()
            } catch (e: Exception) {
                //was previously stopped
            }
            currentClient = if (isRemote) RemoteClient(keystorePath, passphrase)
            else LocalClient(keystorePath, passphrase, pathToLocalNode, networkId)
            true
        }.subscribeOn(scheduler)
                .onErrorReturn { false }
    }

    fun startAndObserve(): Observable<Header> {
        var subscription: Client.HeadersSubscription? = null
        return Observable.fromPublisher<Header> { s ->
            subscription = currentClient.startWithSubscription {
                s.onNext(it)
            }
        }.subscribeOn(scheduler)
                .doOnDispose { subscription?.unsubscribe() }
    }

    fun getBalance(address: Address, blockNum: Long): Observable<BigInt> {
        return Observable.fromCallable<BigInt> {
            currentClient.getBalance(address, blockNum)
        }.subscribeOn(scheduler)
                .onErrorReturn { BigInt(-1) }
    }

    fun sendTo(addressInHex: String): Observable<Boolean> {
        return Observable.fromCallable {
            currentClient.sendEther(Geth.newAddressFromHex(addressInHex), networkId, passphrase)
            true
        }.subscribeOn(scheduler)
                .onErrorReturn { false }
    }

    fun getTokenBalance(contractAddress: String, contractAbi: String): Observable<String> {
        return Observable.fromCallable { currentClient.getTokenBalance(contractAddress, contractAbi) }
                .subscribeOn(scheduler)
                .onErrorReturn { "" }

    }

    fun getAccount(): Account {
        return currentClient.getAccount()
    }
}