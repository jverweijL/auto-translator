package com.liferay.demo.auto.translate;


import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.Destination;
import com.liferay.portal.kernel.messaging.DestinationConfiguration;
import com.liferay.portal.kernel.messaging.DestinationFactory;
import com.liferay.portal.kernel.util.HashMapDictionary;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

/*@Component(immediate = true,
        service = AutoTranslatorConfigurator.class)
public class AutoTranslatorConfigurator {

    protected void activate(BundleContext bundleContext) {
        DestinationConfiguration destinationConfiguration = new DestinationConfiguration(
                DestinationConfiguration.DESTINATION_TYPE_PARALLEL, "spaceDestination/student
                Audit"); destinationConfiguration.setMaximumQueueSize(_MAXIMUM_QUEUE_SIZE);
                Destination destination = destinationFactory.createDestination
                (destinationConfiguration);
        Dictionary<String, Object> properties = new HashMapDictionary<>();
        properties.put("destination.name", destination.getName());
        ServiceRegistration<Destination> serviceRegistration = _bundleContext
                .registerService(Destination.class,
                        destination, properties);
        serviceRegistrations.put(destination.getName(), serviceRegistration);
    }

    @Reference
    private DestinationFactory destinationFactory;

    private final Map<String, ServiceRegistration<Destination>> serviceRegistrations = new HashMap<>();
}*/

@Component (
        immediate = true,
        service = AutoTranslatorConfigurator.class
)
public class AutoTranslatorConfigurator {

    public static final String DESTINATION = "liferay/autotranslator/task";

    @Activate
    protected void activate(BundleContext bundleContext) {
        _log.debug("Howdy, I'm AutoTranslatorConfigurator, here to serve you.");

        _bundleContext = bundleContext;

        // Create a DestinationConfiguration for parallel destinations.

        DestinationConfiguration destinationConfiguration =
                new DestinationConfiguration(
                        DestinationConfiguration.DESTINATION_TYPE_SERIAL,
                        DESTINATION);

        // Set the DestinationConfiguration's max queue size and
        // rejected execution handler.

        destinationConfiguration.setMaximumQueueSize(500);

        /*RejectedExecutionHandler rejectedExecutionHandler =
                new CallerRunsPolicy() {

                    @Override
                    public void rejectedExecution(
                            Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {

                        if (_log.isWarnEnabled()) {
                            _log.warn(
                                    "The current thread will not handle the request " +
                                            "because the graph walker's task queue is at " +
                                            "its maximum capacity");
                        }

                        super.rejectedExecution(runnable, threadPoolExecutor);
                    }

                };

        destinationConfiguration.setRejectedExecutionHandler(
                rejectedExecutionHandler);*/

        // Create the destination
        Destination destination = _destinationFactory.createDestination(destinationConfiguration);
        _log.debug("My destination is " + destination.getName());

        // Add the destination to the OSGi service registry

        Dictionary<String, Object> properties = new HashMapDictionary<>();
        properties.put("destination.name", destination.getName());

        ServiceRegistration<Destination> serviceRegistration =
                _bundleContext.registerService(
                        Destination.class, destination, properties);

        // Track references to the destination service registrations

        _serviceRegistrations.put(destination.getName(),
                serviceRegistration);
    }

    @Deactivate
    protected void deactivate() {

        // Unregister and destroy destinations this component unregistered

        for (ServiceRegistration<Destination> serviceRegistration :
                _serviceRegistrations.values()) {

            Destination destination = _bundleContext.getService(
                    serviceRegistration.getReference());
            _log.debug("Deactivate destination " + destination.getName());

            serviceRegistration.unregister();

            destination.destroy();

        }

        _serviceRegistrations.clear();

    }

    @Reference
    private DestinationFactory _destinationFactory;

    private final Map<String, ServiceRegistration<Destination>>
            _serviceRegistrations = new HashMap<>();

    private volatile BundleContext _bundleContext;

    private static final Log _log = LogFactoryUtil.getLog(
            AutoTranslatorConfigurator.class);

}