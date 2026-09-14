package com.example.appteschi.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * CookieJar en memoria RAM — no persiste en disco (DEC-002).
 */
class InMemoryCookieJar : CookieJar {
    private val store = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val bucket = store.getOrPut(url.host) { mutableListOf() }
        cookies.forEach { cookie ->
            bucket.removeAll { it.name == cookie.name }
            bucket.add(cookie)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        store[url.host]?.filter { it.expiresAt > System.currentTimeMillis() } ?: emptyList()
}
