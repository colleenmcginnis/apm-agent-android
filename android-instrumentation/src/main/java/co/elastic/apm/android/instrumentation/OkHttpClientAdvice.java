package co.elastic.apm.android.instrumentation;

import android.util.Log;

import net.bytebuddy.asm.Advice;

import co.elastic.apm.android.sdk.okhttp.OtelEventListener;
import okhttp3.OkHttpClient;

public class OkHttpClientAdvice {

    @Advice.OnMethodEnter
    public static void enter(@Advice.Argument(0) OkHttpClient.Builder builder) {
        builder.eventListener(new OtelEventListener());
        Log.d("cesar", "Event listener is set");
    }
}