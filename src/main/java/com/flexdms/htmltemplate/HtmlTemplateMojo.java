package com.flexdms.htmltemplate;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

//https://wiki.eclipse.org/M2E_compatible_maven_plugins
/**
 * transform html file into javascript template used by angular js client.
 * 
 * @author jason.zhang
 */
@Mojo(name = "merge", defaultPhase = LifecyclePhase.COMPILE)
public class HtmlTemplateMojo extends AbstractMojo {


	@Component(role=org.sonatype.plexus.build.incremental.BuildContext.class)
	private BuildContext buildContext;

	/**
	 * Source directory to find html fragements
	 */
	@Parameter
	File srcDirectory;

	/**
	 * Template file: expect two tokens:@@@@name@@@@ and @@@@text@@@@@. @@@@name@@@@
	 * will be replaced by html file name. @@@@text@@@@ will be replaced by html
	 * file content.
	 */
	@Parameter(required = false)
	File templateFile;

	/**
	 * javascript produced
	 */
	@Parameter(defaultValue = "src/main/webapp/js/templates.js")
	File finalFile;
	
	/**
	 * String add to the top of the final file.
	 */
	@Parameter
	String fileHeader;
	
	/**
	 * String add to the bottom of the final file before module generation
	 * 
	 */
	@Parameter
	String fileFooter;
	
	/**
	 * Whether to generate module statement at the very bottom of the file.
	 */
	@Parameter
	boolean generateModule=true;
	
	
	
	/**
	 * Whether to process the text
	 */
	@Parameter
	boolean process=true;
	

	public void execute() throws MojoExecutionException {

		Log log = getLog();

		try {
			String template = null;
			if (templateFile != null) {
				log.info("reading template file" + templateFile.getAbsolutePath());
				template = FileUtils.readFileToString(templateFile);
			} else {
				template = IOUtils.toString(HtmlTemplateMojo.class.getClassLoader().getResourceAsStream("com/flexdms/htmltemplate/tpl.js"));
				log.info("Using default template ");

				log.info(template);
			}
			
			log.info("srcDirectory is "+srcDirectory.getAbsolutePath());
			if (!srcDirectory.exists()) {
				log.info("SrcDirectory does not exist");
			}
			Scanner scanner = buildContext.newScanner(srcDirectory, true);
			scanner.scan();
			//scanner.setIncludes(new String[] { "**/*.html" });
			String files[]=scanner.getIncludedFiles();;
			if (files==null ) {
				log.info("no file is changed. Do not renerate template file");
				return;
			}
			java.util.Arrays.sort(files);
			Writer writer = new OutputStreamWriter( buildContext.newFileOutputStream(finalFile));
			List<String> modules = new ArrayList<String>(20);

			String templateNamePrefix = template.substring(template.indexOf('"') + 1, template.indexOf("@@@@name@@@@"));
			if (fileHeader!=null) {
				writer.write(fileHeader);
			}
			for (String childFile: files) {
				transformFile(childFile, srcDirectory, writer, template, modules);
			}
			if (fileFooter!=null) {
				writer.write(fileFooter);
			}
			//handleDirectory(srcDirectory, FilenameUtils.normalizeNoEndSeparator(srcDirectory.getAbsolutePath(), true), writer, template, modules);
			if (generateModule) {
				writer.write("angular.module(\"ui." + srcDirectory.getName() + ".tpls\", [");
				for (String module : modules) {
					writer.write("\"" + templateNamePrefix + module + "\",\n");
				}
				writer.write("]);\n");
			}
			writer.close();
			log.info("Finish writing file " + finalFile.getAbsolutePath());
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}

	}

//	protected void handleDirectory(File dir, String top, Writer writer, String template, List<String> modules) throws FileNotFoundException,
//			IOException {
//		getLog().info("inspect " + srcDirectory.getAbsolutePath() + " for html files");
//		for (File file : dir.listFiles()) {
//			if (file.isDirectory()) {
//				handleDirectory(file, top, writer, template, modules);
//			} else {
//				transformFile(file, top, writer, template, modules);
//			}
//		}
//	}

	protected void transformFile(String fileName, File base, Writer writer, String template, List<String> modules) throws FileNotFoundException,
			IOException {
		String name = FilenameUtils.normalizeNoEndSeparator(fileName, true);
		getLog().info("process " + name);
	
		
		List<String> lines = IOUtils.readLines(new FileReader(new File(base, fileName)));
		StringBuilder sb = new StringBuilder();
		if (process) {
			sb.append("\"\"");
		}
		
		for (String line : lines) {
			if (process)
			{
				String newline = "+\"" + line.replace("\"", "\\\"") + "\"\n";
				sb.append(newline);
			} else {
				sb.append(line);
				sb.append("\n");
			}
		
		}

		String r1 = template.replace("@@@@name@@@@", name);
		String r2 = r1.replace("@@@@text@@@@", sb.toString());

		modules.add(name);

		writer.write("\n\n" + r2);

	}
}
