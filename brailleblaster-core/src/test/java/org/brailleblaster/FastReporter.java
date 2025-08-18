/*
 * Copyright (C) 2025 American Printing House for the Blind
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.brailleblaster;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

/**
 * The TestNG HTML and other reporters are very slow, 
 * however the maven surefire result does not give enough information like full stack traces
 */
public class FastReporter implements IReporter {
	private static final Logger log = LoggerFactory.getLogger(FastReporter.class);

	public FastReporter() {
	}

	@Override
	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
		StringBuilder sb = new StringBuilder();
		for (ISuite suite : suites) {
//			for (Map.Entry<String, ISuiteResult> cur : suite.getResults().entrySet()) {
//				String key = cur.getKey();
//				ISuiteResult value = cur.getValue();
//				System.out.println("key " + key + " value " + value.getTestContext().getSkippedTests().);
//			}
			for (IInvokedMethod curResult : suite.getAllInvokedMethods()) {
				process(sb, curResult.getTestResult());
			}
			for (ITestNGMethod curMEthod : suite.getExcludedMethods()) {
				sb.append("MISSING ");
				addMethod(sb, curMEthod);
			}
		}
		
		File resultFile = new File(outputDirectory, "fast-results.txt");
		log.info("Writing test results to " + resultFile.getAbsolutePath());
		try {
			FileUtils.write(resultFile, sb, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new RuntimeException("Failed to write to " + resultFile.getAbsolutePath(), e);
		}
	}
	
	private void addMethod(StringBuilder output, ITestNGMethod method) {
		output.append(method.getRealClass().getName())
				.append(".")
				.append(method.getMethodName());

		if (method.getGroups().length != 0) {
			output.append(" Groups: ")
					.append(String.join(",", method.getGroups()));
		}

		if (method.getDescription() != null) {
			output.append(" - ")
				.append(method.getDescription());
		}
		
		output.append(System.lineSeparator());
	}

	private void process(StringBuilder output, ITestResult result) {
		log.trace("Processing " + result.getMethod().getMethodName());
		Result resultCode = Result.fromTestResult(result.getStatus());
		if (resultCode == Result.SUCCESS) {
			return;
		}
		output.append(resultCode.name())
				.append(" ");
		addMethod(output, result.getMethod());
				

		if (resultCode == Result.FAILURE) {
			if (result.getThrowable() == null) {
				output.append("No exception?");
			} else {
				output.append(ExceptionUtils.getStackTrace(result.getThrowable()));
			}
		} else if (resultCode == Result.SKIP) {
			output.append("Skipped!");
		}
		
		output.append(System.lineSeparator());

		log.info("result " + result.getInstanceName() + " | " + result.getStatus(), result.getThrowable());
	}

	enum Result {
		SUCCESS(1),
		FAILURE(2),
		SKIP(3);

		private final int statusCode;

		Result(int statusCode) {
			this.statusCode = statusCode;
		}

		public static Result fromTestResult(int code) {
			for (Result curValue : values()) {
				if (curValue.statusCode == code) {
					return curValue;
				}
			}
			throw new RuntimeException("Unknown code " + code);
		}
	}
}
