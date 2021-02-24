package test;

import java.util.Properties;
import test.api.Hello;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceRegistration;

public class HelloImpl implements Hello, BundleActivator {

	private ServiceRegistration registration;

	public void start(BundleContext context) {
		Properties props = new Properties();
		props.put("Language", "English");
		registration = context.registerService(Hello.class.getName(), this, props);
    }

	public void stop(BundleContext context) {

    }

	public String sayHello(String name) {
		return "Hello "+name;
	}

}

