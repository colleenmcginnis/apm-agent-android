package co.elastic.apm.android.sdk.utility;

import android.content.Context;
import android.os.Build;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import co.elastic.apm.android.sdk.BuildConfig;
import co.elastic.apm.android.tools.ApmInfo;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

public class ResourcesProvider {

    public static Attributes getCommonResourceAttributes(Context appContext) {
        Properties apmInfoProperties = getApmInfoProperties(appContext);
        return Attributes.builder()
                .put(ResourceAttributes.SERVICE_NAME, appContext.getPackageName())
                .put(ResourceAttributes.SERVICE_VERSION, apmInfoProperties.getProperty(ApmInfo.KEY_VERSION))
                .put("telemetry.sdk.name", "android")
                .put("telemetry.sdk.version", BuildConfig.APM_AGENT_VERSION)
                .put("telemetry.sdk.language", "java")
                .put(ResourceAttributes.OS_DESCRIPTION, getOsDescription())
                .put(ResourceAttributes.OS_TYPE, "linux")
                .put(ResourceAttributes.OS_VERSION, Build.VERSION.RELEASE)
                .put(ResourceAttributes.OS_NAME, Build.VERSION.CODENAME)
                .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, apmInfoProperties.getProperty(ApmInfo.KEY_VARIANT_NAME))
                .put(ResourceAttributes.DEVICE_ID, DeviceIdProvider.getDeviceId(appContext))
                .put(ResourceAttributes.DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                .put(ResourceAttributes.DEVICE_MANUFACTURER, Build.MANUFACTURER)
                .build();
    }

    private static String getOsDescription() {
        StringBuilder descriptionBuilder = new StringBuilder();
        descriptionBuilder.append("Android ");
        descriptionBuilder.append(Build.VERSION.RELEASE);
        descriptionBuilder.append(", API level ");
        descriptionBuilder.append(Build.VERSION.SDK_INT);
        descriptionBuilder.append(", NAME ");
        descriptionBuilder.append(Build.VERSION.CODENAME);
        descriptionBuilder.append(", BUILD ");
        descriptionBuilder.append(Build.VERSION.INCREMENTAL);
        return descriptionBuilder.toString();
    }

    private static Properties getApmInfoProperties(Context appContext) {
        try (InputStream propertiesFileInputStream = appContext.getAssets().open(ApmInfo.ASSET_FILE_NAME)) {
            Properties properties = new Properties();
            properties.load(propertiesFileInputStream);
            return properties;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}