package com.dataart.aakimov.ethernode

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.dataart.aakimov.ethernode.bl.ClientManager
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxCompoundButton
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var clientManager: ClientManager
    private lateinit var subscriptions: CompositeDisposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clientManager = ClientManager(this)
        subscriptions = CompositeDisposable()

        setContentView(R.layout.activity_main)
        syncStateChanged(false)
        remoteCheck.isChecked = true

        subscriptions.add(RxCompoundButton.checkedChanges(remoteCheck)
                .skipInitialValue()
                .flatMap { isChecked -> clientManager.switch(isChecked) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ syncStateChanged(!it) }, {
                    Toast.makeText(this, R.string.switch_node_error, Toast.LENGTH_SHORT).show()
                }))

        subscriptions.add(RxView.clicks(sync)
                .doOnNext {
                    latestBlockInfo.setText(R.string.syncing)
                    syncStateChanged(true)
                }.flatMap {
                    clientManager.startAndObserve()
                }.observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    latestBlockInfo.text = "# ${it.number} ${it.hash.hex.substring(0, 10)}..."
                }, {
                    syncStateChanged(false)
                    Toast.makeText(this, R.string.sync_error, Toast.LENGTH_SHORT)
                            .show()
                }))

        val accClick = RxView.clicks(getAccount).share()
        subscriptions.add(accClick
                .map {
                    val address = clientManager.getAccount().address
                    accountAddress.text = address.hex
                    address
                }.flatMap { clientManager.getBalance(it, -1) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    balance.text = if (it.sign() < 0) getString(R.string.error) else it
                            .toString()
                }, {
                    Toast.makeText(this, R.string.get_balance_error, Toast.LENGTH_SHORT).show()
                })
        )

        subscriptions.add(accClick
                .map {
                    val address = clientManager.getAccount().address
                    accountAddress.text = address.hex
                    address
                }.flatMap {
                    clientManager.getTokenBalance(tokenContractAddress.text.toString(),
                            tokenContractAbi.text.toString())
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    balanceInToken.text = if (it.isEmpty()) getString(R.string.error) else it
                }, {
                    Toast.makeText(this, R.string.get_token_balance_error, Toast.LENGTH_SHORT)
                            .show()
                })
        )

        subscriptions.add(RxView.clicks(sendTo)
                .doOnNext {
                    sendTo.isEnabled = false
                }.flatMap { clientManager.sendTo(toAddress.text.toString()) }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { sendTo.isEnabled = true }
                .subscribe({ success ->
                    if (!success) {
                        Toast.makeText(this, R.string.send_ether_error, Toast.LENGTH_SHORT).show()
                    }
                }, {
                    Toast.makeText(this, R.string.send_ether_error, Toast.LENGTH_SHORT).show()
                }))
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.dispose()
    }

    private fun syncStateChanged(isSyncing: Boolean) {
        sync.isEnabled = !isSyncing
        getAccount.isEnabled = isSyncing
        sendTo.isEnabled = isSyncing
        remoteCheck.isEnabled = isSyncing
    }
}
