package org.nalby.yobatis.structure.eclipse;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import org.dom4j.DocumentException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.nalby.yobatis.exception.ProjectException;
import org.nalby.yobatis.exception.ProjectNotFoundException;
import org.nalby.yobatis.structure.Folder;
import org.nalby.yobatis.structure.Project;
import org.nalby.yobatis.structure.SourceCodeFolder;
import org.nalby.yobatis.util.Expect;
import org.nalby.yobatis.xml.RootPomXmlParser;
import org.nalby.yobatis.xml.RootSpringXmlParser;
import org.nalby.yobatis.xml.WebXmlParser;

public class EclipseProject extends Project {

	private IProject wrappedProject;

	private RootPomXmlParser pom;

	private RootSpringXmlParser spring;

	private WebXmlParser web;
	
	private SourceCodeFolder sourceCodeFolder;
	
	private EclipseProject(IProject project, RootPomXmlParser pom,
			RootSpringXmlParser spring, WebXmlParser web, SourceCodeFolder sourceCodeFolder) {
		this.wrappedProject = project;
		this.pom = pom;
		this.spring = spring;
		this.web = web;
		this.sourceCodeFolder = sourceCodeFolder;
	}

	@Override
	public String getDatabaseUrl() {
		return spring.getDbUrl();
	}

	@Override
	public String getDatabaseUsername() {
		return spring.getDbUsername();
	}

	@Override
	public String getDatabasePassword() {
		return spring.getDbPassword();
	}

	@Override
	public String getDatabaseDriverClassName() {
		return spring.getDbDriverClass();
	}
	
	private static String getServletConfigPath(WebXmlParser webXmlParser) throws DocumentException {
		Set<String> servletConfigPath = webXmlParser.getServletConfigLocation();
		if (servletConfigPath.size() != 1) {
			throw new ProjectException("Should have only one servlet config.");
		}
		for (String path: servletConfigPath) {
			return path.replace(CLASSPATH_PREFIX, MAVEN_RESOURCES_PATH);
		}
		return null;
	}
	
	private static RootSpringXmlParser getSpringXmlParser(IProject project, WebXmlParser webXmlParser) throws DocumentException, FileNotFoundException, IOException {
		String appConfigPath = webXmlParser.getAppConfigLocation();
		RootSpringXmlParser springXmlParser = null;
		if (appConfigPath != null) {
			appConfigPath.replace(CLASSPATH_PREFIX, MAVEN_RESOURCES_PATH);
			springXmlParser  = new RootSpringXmlParser(new FileInputStream(project.getLocationURI().getPath() + "/" + appConfigPath));
		}
		String servletConfigPath = getServletConfigPath(webXmlParser);
		if (servletConfigPath != null) {
			if (springXmlParser != null) {
				springXmlParser.appendSpringXmlConfig(new FileInputStream(project.getLocationURI().getPath() + "/" + servletConfigPath));
			} else {
				springXmlParser = new RootSpringXmlParser(new FileInputStream(project.getLocationURI().getPath() + "/" + servletConfigPath));
			}
		}
		if (springXmlParser == null) {
			throw new ProjectException("No spring xml file detected.");
		}
		return springXmlParser;
	}

	public static EclipseProject build(String name) {
		Expect.notEmpty(name, "project name must not be null.");
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(name);
		if (!project.exists()) {
			throw new ProjectNotFoundException();
		}
		try {
			if (project.exists() && !project.isOpen()) {
				project.open(null);
			}
			IFolder ifolder = project.getFolder(MAVEN_SOURCE_CODE_PATH);
			SourceCodeFolder sourceCodeFolder = new SourceCodeFolder(new EclipseFolder(null, ifolder));

			WebXmlParser webXmlParser = new WebXmlParser(new FileInputStream(project.getLocationURI().getPath() + "/" + WEB_XML_PATH));
			RootSpringXmlParser springXmlParser = getSpringXmlParser(project, webXmlParser);
			return new EclipseProject(project, null, springXmlParser, webXmlParser, sourceCodeFolder);
		} catch (Exception e) {
			throw new ProjectException(e.getMessage());
		}
	}

}
