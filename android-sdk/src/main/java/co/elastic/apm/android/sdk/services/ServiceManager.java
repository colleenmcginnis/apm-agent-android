package co.elastic.apm.android.sdk.services;

import java.util.HashMap;

public final class ServiceManager implements Lifecycle {
    private final HashMap<String, Service> services = new HashMap<>();

    public void addService(Service service) {
        String name = service.name();
        verifyNotExisting(name);
        services.put(name, service);
    }

    @Override
    public void start() {
        for (Service service : services.values()) {
            service.start();
        }
    }

    @Override
    public void stop() {
        for (Service service : services.values()) {
            service.stop();
        }
    }

    public Service getByName(String name) {
        Service service = services.get(name);
        if (service == null) {
            throw new IllegalArgumentException("Service not found: " + name);
        }

        return service;
    }

    private void verifyNotExisting(String name) {
        if (services.containsKey(name)) {
            throw new IllegalArgumentException("Service already registered with name: " + name);
        }
    }
}
