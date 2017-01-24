/*******************************************************************************
 * Work Binder Application Service
 * Copyright 2004, 2009 University of Belgrade Computer Centre and
 * individual contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of individual
 * contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software.  If not, see:
 * http://www.gnu.org/licenses/lgpl.txt
 *******************************************************************************/
package yu.ac.bg.rcub.binder.handler;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

import yu.ac.bg.rcub.binder.BinderUtil;

/**
 * Dynamically loads supported application settings.
 * 
 * @author choppa
 * 
 */
public class AppLoader {

	private boolean appPropRead = false;
	private String appName = null;
	private String appHandler = null;
	private String appLibs = null;
	private String appParams = null;
	private File appDir = null;

	private static final String LIB_DIR = "lib";
	private static final String JAR_EXT = ".jar";
	static final String DEFAULT_PROP_FILENAME = "binder-plugin.properties";

	private String[] searchDirs;
	private String appsPropFile = DEFAULT_PROP_FILENAME;

	public AppLoader(String[] dirs, String appPropsFileName) {
		this.searchDirs = dirs;
		if (appPropsFileName != null)
			this.appsPropFile = appPropsFileName;
	}

	/**
	 * Creates a listing of supported applications.
	 * 
	 * @return A <code>String</code> containing application names.
	 */
	public String getAppsListing() {
		File[] dirList = getAppDirs();
		FileFilter nameFilter = getPropFileFilter();
		String appsList = "";
		for (int i = 0; i < dirList.length; i++) {
			File[] propFile = dirList[i].listFiles(nameFilter);
			if (propFile != null && propFile.length == 1) {
				String appName = getAppProp(propFile[0]).getProperty("ApplicationName");
				if (appName != null)
					appsList += i == 0 ? appName : " " + appName;
			}
		}
		return appsList;
	}

	/**
	 * Searches for the application, and tries to read its properties file. If
	 * it finds it, application parameters are read and available.
	 * 
	 * @param appID
	 *            <code>String</code> representing application name.
	 * @return <code>true</code> if the properties file containing application
	 *         name was located, otherwise false.
	 */
	public boolean readAppConfig(String appID) {
		File[] dirList = getAppDirs();
		FileFilter nameFilter = getPropFileFilter();
		for (int i = 0; i < dirList.length; i++) {
			File[] propFile = dirList[i].listFiles(nameFilter);
			if (propFile != null && propFile.length == 1) {
				Properties p = getAppProp(propFile[0]);
				String appName = p.getProperty("ApplicationName");
				if (appName != null && appName.compareTo(appID) == 0) {
					this.appName = appName;
					appDir = dirList[i];
					appHandler = p.getProperty(appName + "_Class");
					appLibs = p.getProperty(appName + "_Libs");
					appParams = p.getProperty(appName + "_Parameters");
					return appPropRead = true;
				}
			}
		}
		return false;
	}

	/**
	 * The list of dirs in the application root directory.
	 * 
	 * @return The array of <code>File</code> representing a list of
	 *         directories.
	 */
	private File[] getAppDirs() {
		if (searchDirs == null)
			return null;

		/* Filter that accepts only directories. */
		FileFilter filter = new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory();
			}
		};
		/* go through all dirs */
		File[] results = new File[0];
		for (String swDir : searchDirs) {
			/* get sw_dir from env */
			if (swDir != null && !swDir.equals("")) {
				File dir = new File(swDir);
				if (dir.exists() && dir.isDirectory() && dir.canRead())
					results = addFiles(results, dir.listFiles(filter));
			}
		}
		return results;
	}

	/**
	 * A <code>FileFilter</code> used to filter out app properties file.
	 * 
	 * @return A <code>FileFilter</code> that will filter out app properties
	 *         file.
	 */
	private FileFilter getPropFileFilter() {
		/* Search only dirs that contain a readable properties file. */
		return new FileFilter() {
			public boolean accept(File file) {
				return file.isFile() && file.canRead() && file.getName().equals(appsPropFile);
			}
		};
	}

	private FileFilter getJarFileFilter() {
		/* Search for *.jar files. */
		return new FileFilter() {
			public boolean accept(File file) {
				return file.isFile() && file.canRead() && file.getName().endsWith(JAR_EXT);
			}
		};
	}

	/**
	 * Loads the application properties file.
	 * 
	 * @param file
	 *            Application properties <code>File</code>.
	 * @return A <code>Properties</code> object representing the application
	 *         properties.
	 */
	private Properties getAppProp(File file) {
		Properties p = new Properties();
		try {
			p.load(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			/* Should not happen, because we filtered files. */
			throw new RuntimeException("File not found.", e);
		} catch (IOException e) {
			/* Should not happen, because we checked read access. */
			throw new RuntimeException("File not readable.", e);
		}
		return p;
	}

	private File[] addFiles(File[] a, File[] b) {
		final int alen = a.length;
		final int blen = b.length;
		final File[] result = new File[alen + blen];
		System.arraycopy(a, 0, result, 0, alen);
		System.arraycopy(b, 0, result, alen, blen);
		return result;
	}

	public ClassLoader getClassLoader() {
		if (!appPropRead)
			return null;

		ArrayList<URL> urls = new ArrayList<URL>();
		/* 1st - add jars from /lib */
		File libDir = new File(appDir.getAbsolutePath() + File.separator + LIB_DIR);
		File[] jars = libDir.listFiles(getJarFileFilter());
		if (jars != null)
			for (File f : jars) {
				try {
					urls.add(f.toURI().toURL());
				} catch (MalformedURLException e) {
					// ignore
				}
			}
		/* 2nd - add additional jars */
		if (appLibs != null && !appLibs.equals("")) {
			/* Better then String.split() solution. Dont include quotes. */
			String[] libArray = BinderUtil.readArgs(appLibs, false);
			for (int i = 0; i < libArray.length; i++)
				try {
					urls.add(new URL(libArray[i]));
				} catch (MalformedURLException e) {
					// log("Warning! Bad lib spec: '" + libArray[i] + "'.", e);
				}
		}
		PluginClassLoader cl = new PluginClassLoader(urls.toArray(new URL[urls.size()]));
		try {
			/* add permissions to classloader */
			cl.addPermission(appDir.getCanonicalFile().toString());
		} catch (IOException e) {
			// ignore
		}
		return cl;
	}

	public boolean isAppAvailable() {
		return appPropRead;
	}

	public String getAppName() {
		return appPropRead ? appName : null;
	}

	public String getAppWorkerHandler() {
		return appPropRead ? appHandler : null;
	}

	public String getAppLibs() {
		return appPropRead ? appLibs : null;
	}

	public String getAppParams() {
		return appPropRead ? appParams : null;
	}

	public File getAppDir() {
		return appPropRead ? appDir : null;
	}

}
