/*******************************************************************************
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 *******************************************************************************/

package com.liferay.ide.eclipse.server.tomcat.core;

import com.liferay.ide.eclipse.core.util.CoreUtil;
import com.liferay.ide.eclipse.project.core.util.ProjectUtil;
import com.liferay.ide.eclipse.server.core.IPluginPublisher;
import com.liferay.ide.eclipse.server.core.LiferayServerCorePlugin;
import com.liferay.ide.eclipse.server.tomcat.core.util.ExternalPropertiesConfiguration;
import com.liferay.ide.eclipse.server.util.ServerUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.jst.server.tomcat.core.internal.Tomcat60Handler;
import org.eclipse.jst.server.tomcat.core.internal.TomcatVersionHelper;
import org.eclipse.jst.server.tomcat.core.internal.xml.server40.ServerInstance;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;

/**
 * @author Greg Amerson
 */
@SuppressWarnings("restriction")
public class LiferayTomcat60Handler extends Tomcat60Handler {

	protected IServer currentServer;

	protected ILiferayTomcatServer portalServer;

	@Override
	public IStatus canAddModule(IModule module) {
		IProject project = module.getProject();

		if (project != null) {
			IFacetedProject facetedProject = ProjectUtil.getFacetedProject(project);

			if (facetedProject != null) {
				IProjectFacet liferayFacet = ProjectUtil.getLiferayFacet(facetedProject);

				if (liferayFacet != null) {
					String facetId = liferayFacet.getId();

					IRuntime runtime = null;

					try {
						runtime = ServerUtil.getRuntime(project);
					}
					catch (CoreException e) {
					}

					if (runtime != null) {
						IPluginPublisher pluginPublisher =
							LiferayServerCorePlugin.getPluginPublisher(facetId, runtime.getRuntimeType().getId());

						if (pluginPublisher != null) {
							IStatus status = pluginPublisher.canPublishModule(currentServer, module);

							if (!status.isOK()) {
								return status;
							}
						}
					}
				}
			}
		}

		return super.canAddModule(module);
	}

	@Override
	public String[] getRuntimeVMArguments(IPath installPath, IPath configPath, IPath deployPath, boolean isTestEnv) {
		List<String> runtimeVMArgs = new ArrayList<String>();

		addUserVMArgs(runtimeVMArgs);

		runtimeVMArgs.add("-Dfile.encoding=UTF8");
		runtimeVMArgs.add("-Dorg.apache.catalina.loader.WebappClassLoader.ENABLE_CLEAR_REFERENCES=false");
		runtimeVMArgs.add("-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager");
		runtimeVMArgs.add("-Djava.security.auth.login.config=\"" + configPath.toOSString() + "/conf/jaas.config\"");
		runtimeVMArgs.add("-Djava.util.logging.config.file=\"" + installPath.toOSString() +
			"/conf/logging.properties\"");
		runtimeVMArgs.add("-Djava.io.tmpdir=\"" + installPath.toOSString() + "/temp\"");

		File externalPropertiesFile = getExternalPropertiesFile(installPath, configPath);

		runtimeVMArgs.add("-Dexternal-properties=\"" + externalPropertiesFile.getAbsolutePath() + "\"");

		Collections.addAll(runtimeVMArgs, super.getRuntimeVMArguments(installPath, configPath, deployPath, isTestEnv));

		return runtimeVMArgs.toArray(new String[runtimeVMArgs.size()]);
	}

	public void setCurrentServer(IServer server) {
		this.currentServer = server;
	}

	private void addUserVMArgs(List<String> runtimeVMArgs) {
		String[] memoryArgs = ILiferayTomcatConstants.DEFAULT_MEMORY_ARGS.split(" ");
		String userTimezone = ILiferayTomcatConstants.DEFAULT_USER_TIMEZONE;

		if (currentServer != null) {
			ILiferayTomcatServer portalTomcatServer = getPortalServer();

			memoryArgs = DebugPlugin.parseArguments(portalTomcatServer.getMemoryArgs());

			userTimezone = portalTomcatServer.getUserTimezone();
		}

		if (memoryArgs != null) {
			for (String arg : memoryArgs) {
				runtimeVMArgs.add(arg);
			}
		}

		runtimeVMArgs.add("-Duser.timezone=" + userTimezone);
	}

	private File getExternalPropertiesFile(IPath installPath, IPath configPath) {
		File retval = null;

		ILiferayTomcatServer portalServer = getPortalServer();

		if (portalServer != null) {
			File portalIdePropFile = ensurePortalIDEPropertiesExists(installPath, configPath);

			retval = portalIdePropFile;

			String externalProperties = portalServer.getExternalProperties();

			if (!CoreUtil.isNullOrEmpty(externalProperties)) {
				File externalPropertiesFile = setupExternalPropertiesFile(portalIdePropFile, externalProperties);

				if (externalPropertiesFile != null) {
					retval = externalPropertiesFile;
				}
			}
		}

		return retval;
	}

	protected File ensurePortalIDEPropertiesExists(IPath installPath, IPath configPath) {

		IPath idePropertiesPath = installPath.append("../portal-ide.properties");

		String hostName = "localhost";

		try {
			ServerInstance server =
				TomcatVersionHelper.getCatalinaServerInstance(configPath.append("conf/server.xml"), null, null);

			hostName = server.getHost().getName();
		}
		catch (Exception e) {
			LiferayTomcatPlugin.logError(e);
		}

		// read portal-developer.properties
		// Properties devProps = new Properties();
		// IPath devPropertiesPath =
		// installPath.append("webapps/ROOT/WEB-INF/classes/portal-developer.properties");
		// if (devPropertiesPath.toFile().exists()) {
		// devProps.load(new FileReader(devPropertiesPath.toFile()));
		// }

		// if (idePropertiesPath.toFile().exists()) {
		// String value =
		// CoreUtil.readPropertyFileValue(idePropertiesPath.toFile(),
		// "auto.deploy.tomcat.conf.dir");
		// if (configPath.append("conf/Catalina/"+hostName).toFile().equals(new
		// File(value))) {
		// return;
		// }
		// }

		Properties props = new Properties();

		props.put( "include-and-override", "portal-developer.properties" );

		props.put("auto.deploy.tomcat.conf.dir", configPath.append("conf/Catalina/" + hostName).toOSString());

		if (this.currentServer != null) {
			ILiferayTomcatServer portalServer = getPortalServer();

			if (portalServer != null) {
				IPath runtimLocation = getServer().getRuntime().getLocation();

				String autoDeployDir = portalServer.getAutoDeployDirectory();

				if (!ILiferayTomcatConstants.DEFAULT_AUTO_DEPLOYDIR.equals(autoDeployDir)) {
					IPath autoDeployDirPath = new Path(autoDeployDir);

					if (autoDeployDirPath.isAbsolute() && autoDeployDirPath.toFile().exists()) {
						props.put("auto.deploy.deploy.dir", portalServer.getAutoDeployDirectory());
					}
					else {
						File autoDeployDirFile = new File(runtimLocation.toFile(), autoDeployDir);

						if (autoDeployDirFile.exists()) {
							props.put("auto.deploy.deploy.dir", autoDeployDirFile.getPath());
						}
					}
				}

				props.put("auto.deploy.interval", portalServer.getAutoDeployInterval());
			}
		}

		File file = idePropertiesPath.toFile();

		try {
			props.store(new FileOutputStream(file), null);
		}
		catch (Exception e) {
			LiferayTomcatPlugin.logError(e);
		}

		return file;
	}

	protected ILiferayTomcatServer getPortalServer() {
		if (this.portalServer == null) {
			this.portalServer = (ILiferayTomcatServer) getServer().loadAdapter(ILiferayTomcatServer.class, null);
		}

		return this.portalServer;
	}

	protected IServer getServer() {
		return this.currentServer;
	}

	protected File setupExternalPropertiesFile(File portalIdePropFile, String externalPropertiesPath) {
		File retval = null;
		// first check to see if there is an external properties file
		File externalPropertiesFile = new File(externalPropertiesPath);

		if (externalPropertiesFile.exists()) {
			ExternalPropertiesConfiguration props = new ExternalPropertiesConfiguration();
			try {
				props.load(new FileInputStream(externalPropertiesFile));

				props.setProperty("include-and-override", portalIdePropFile.getAbsolutePath());

				props.setHeader("# Last modified by Liferay IDE " + new Date());

				props.save(new FileOutputStream(externalPropertiesFile));

				retval = externalPropertiesFile;
			}
			catch (Exception e) {
				retval = null;
			}
		}
		else {
			retval = null; // don't setup an external properties file
		}

		return retval;
	}

}
