package com.tilal6991.channels.base

import android.content.Context
import android.view.View
import com.brianegan.bansa.Store
import com.tilal6991.channels.redux.RxStore
import com.tilal6991.channels.redux.state.GlobalState
import com.tilal6991.channels.ui.Presenter
import com.tilal6991.channels.viewmodel.RelayVM
import rx.Observable

val Context.app: ChannelsApplication
    get() = applicationContext as ChannelsApplication

val Context.relayVM: RelayVM
    get() = app.relayHandle
val Presenter.relayVM: RelayVM
    get() = context.relayVM

val Context.store: RxStore<GlobalState>
    get() = app.store

val Context.storeEvents: Observable<GlobalState>
    get() = app.state