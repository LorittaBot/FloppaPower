package net.perfectdreams.floppapower.utils.extensions

import kotlinx.coroutines.future.await
import net.dv8tion.jda.api.requests.RestAction

suspend fun <T> RestAction<T>.await() : T = this.submit().await()