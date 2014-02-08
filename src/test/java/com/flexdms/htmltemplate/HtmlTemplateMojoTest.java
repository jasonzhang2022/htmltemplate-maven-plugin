package com.flexdms.htmltemplate;

import java.io.File;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Test;

import com.flexdms.htmltemplate.HtmlTemplateMojo;

public class HtmlTemplateMojoTest extends AbstractMojoTestCase {

	protected void setUp() throws Exception {
		// required
		super.setUp();
	}

	protected void tearDown() throws Exception {
		// required
		super.tearDown();
	}

	@Test
	public void test() throws Exception {
		new File("target/final.js").delete();
		File pom = getTestFile("src/test/resources/unit/testpom.xml");
		assertNotNull(pom);
		assertTrue(pom.exists());

		HtmlTemplateMojo myMojo = (HtmlTemplateMojo) lookupMojo("merge", pom);
		assertNotNull(myMojo);
		myMojo.execute();
		assertTrue(new File("target/final.js").exists());
		
		
		String contentString=FileUtils.readFileToString(new File("target/final.js"));
		System.out.println(contentString);
		assertTrue(contentString.indexOf("angular.module(\"template/parent/parent1.html\"")!=-1);
		assertTrue(contentString.indexOf("angular.module(\"template/parent/child/child1.html\"")!=-1);
		assertTrue(contentString.indexOf("angular.module(\"template/parent/child/child2.html\"")!=-1);
		assertTrue(contentString.contains("+\"<div id=\\\"child1\\\">\""));
		
	
	}

}
