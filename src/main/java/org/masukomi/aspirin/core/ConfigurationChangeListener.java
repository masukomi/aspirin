package org.masukomi.aspirin.core;

/**
 * <p>This interface is part of configuration subsystem. If a configuration 
 * parameter was changed, the classes which implements this interface could be 
 * notified about these changes. It is requried to allow immediately dynamic 
 * configuration.</p>
 *
 * @version $Id$
 *
 */
interface ConfigurationChangeListener {
	/**
	 * This method is called when a configuration parameter is changed.
	 * @param parameterName Name of changed parameter.
	 */
	public void configChanged(String parameterName);
}
