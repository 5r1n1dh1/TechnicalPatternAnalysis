package com.tpa.app

import android.app.Application
import android.content.Context
import com.tpa.app.data.wishlist.WishlistRepository
import com.tpa.app.network.NetworkModule
import com.tpa.app.network.TpaApi

/** Process-wide dependencies: one Retrofit client, one Room database. */
class AppContainer(context: Context) {
    val api: TpaApi = NetworkModule.createApi()
    val wishlist: WishlistRepository = WishlistRepository(context)
}

class TpaApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
