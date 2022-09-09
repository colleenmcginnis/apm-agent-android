package co.elastic.apm.android.sdk.traces.okhttp.compose;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.EventListener;

public class CompositeEventListenerFactory implements EventListener.Factory {
    private final List<EventListener.Factory> factories;

    public CompositeEventListenerFactory(List<EventListener.Factory> factories) {
        this.factories = factories;
    }

    @NonNull
    @Override
    public EventListener create(@NonNull Call call) {
        List<EventListener> listeners = new ArrayList<>();

        for (EventListener.Factory factory : factories) {
            listeners.add(factory.create(call));
        }

        return new CompositeEventListener(listeners);
    }
}