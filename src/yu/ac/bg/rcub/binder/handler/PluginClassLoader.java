package yu.ac.bg.rcub.binder.handler;

import java.io.File;
import java.io.FilePermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * ClassLoader for Work Binder plugins.
 * 
 * @author choppa
 * 
 */
public class PluginClassLoader extends URLClassLoader {

	private SecurityManager securityManager;

	/**
	 * The PermissionCollection for each CodeSource for a web application
	 * context.
	 */
	protected HashMap<String, PermissionCollection> loaderPC = new HashMap<String, PermissionCollection>();

	/**
	 * A list of read File Permission required plugin context.
	 */
	protected ArrayList<Permission> permissionList = new ArrayList<Permission>();

	public PluginClassLoader(URL[] urls) {
		super(urls);
		securityManager = System.getSecurityManager();
	}

	/**
	 * Get the Permissions for a CodeSource. Read FilePermission for the plugin
	 * base directory is added.
	 * 
	 * @param codeSource
	 *            where the code was loaded from
	 * @return PermissionCollection for CodeSource
	 */
	protected PermissionCollection getPermissions(CodeSource codeSource) {

		String codeUrl = codeSource.getLocation().toString();
		PermissionCollection pc;
		if ((pc = loaderPC.get(codeUrl)) == null) {
			pc = super.getPermissions(codeSource);
			if (pc != null) {
				Iterator<Permission> perms = permissionList.iterator();
				while (perms.hasNext()) {
					Permission p = perms.next();
					pc.add(p);
				}
				loaderPC.put(codeUrl, pc);
			}
		}
		return pc;
	}

	/**
	 * If there is a Java SecurityManager create a read FilePermission for
	 * plugin file directory path.
	 * 
	 * @param path
	 *            file directory path
	 */
	public void addPermission(String path) {
		if (path == null) {
			return;
		}

		if (securityManager != null) {
			Permission permission = null;
			if (!path.endsWith(File.separator)) {
				permission = new FilePermission(path, "read");
				addPermission(permission);
				path = path + File.separator;
			}
			permission = new FilePermission(path + "-", "read");
			addPermission(permission);
		}
	}

	/**
	 * If there is a Java SecurityManager create a read FilePermission for URL.
	 * 
	 * @param url
	 *            URL for a file or directory on local system
	 */
	public void addPermission(URL url) {
		if (url != null) {
			addPermission(url.toString());
		}
	}

	/**
	 * If there is a Java SecurityManager create a Permission.
	 * 
	 * @param permission
	 *            The permission
	 */
	public void addPermission(Permission permission) {
		if (securityManager != null && permission != null) {
			permissionList.add(permission);
		}
	}

}
