package com.bioacupunt.data.remote

import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Regression tests for the launch crash.
 *
 * History: `authInterceptor` and `hostInterceptor` were `lateinit`, assigned only
 * inside [RetrofitInstance.init]. Nothing ever called `init()`, so resolving
 * `authApi` (which AppContainer does while building AuthRepository) evaluated the
 * `okHttpClient` lazy, hit an unset `lateinit`, and threw
 * `UninitializedPropertyAccessException` — the app died before its first frame.
 *
 * `initIsNotRequired` fails against the old implementation and passes against the
 * fixed one. It is the test that would have caught this before the APK shipped.
 */
class RetrofitInstanceTest {

    /**
     * The network graph must not depend on [RetrofitInstance.init] having run.
     * No `init()` call here — deliberately.
     */
    @Test
    fun initIsNotRequired_apisResolveWithoutThrowing() {
        assertNotNull(RetrofitInstance.authApi)
        assertNotNull(RetrofitInstance.api)
    }

    /** Configuring after the APIs are already resolved must stay safe. */
    @Test
    fun initAfterApisResolved_doesNotThrow() {
        assertNotNull(RetrofitInstance.authApi)

        RetrofitInstance.init(
            tokenProvider = { "test-token" },
            serverUrlProvider = { "https://staging.example.com/" },
        )

        assertNotNull(RetrofitInstance.authApi)
        assertNotNull(RetrofitInstance.api)
    }

    /** A blank server URL must fall back to the default, never produce a bad host. */
    @Test
    fun blankServerUrl_doesNotThrow() {
        RetrofitInstance.init(
            tokenProvider = { "" },
            serverUrlProvider = { "" },
        )

        assertNotNull(RetrofitInstance.api)
    }
}
