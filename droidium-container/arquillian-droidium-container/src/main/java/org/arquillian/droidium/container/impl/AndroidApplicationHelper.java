/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.arquillian.droidium.container.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.arquillian.droidium.container.api.AndroidExecutionException;
import org.arquillian.droidium.container.configuration.AndroidSDK;
import org.arquillian.droidium.container.configuration.Validate;
import org.arquillian.spacelift.process.Command;
import org.arquillian.spacelift.process.CommandBuilder;
import org.arquillian.spacelift.process.ProcessExecution;
import org.arquillian.spacelift.process.ProcessExecutor;

/**
 * Provides various helper methods for Android packages.
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public class AndroidApplicationHelper {

    private static final Logger logger = Logger.getLogger(AndroidApplicationHelper.class.getName());

    private final ProcessExecutor executor;

    private final AndroidSDK sdk;

    /**
     *
     * @param executor
     * @param sdk
     * @throws IllegalArgumentException if either {@code Executor} or {@code sdk} is a null object
     */
    public AndroidApplicationHelper(ProcessExecutor executor, AndroidSDK sdk) {
        Validate.notNull(executor, "Process executor to set can not be a null object!");
        Validate.notNull(sdk, "Android SDK to set can not be a null object!");
        this.executor = executor;
        this.sdk = sdk;
    }

    /**
     *
     * @param apk apk file to get the name of main activity from
     * @return name of main activity of application
     */
    public String getApplicationMainActivity(File apk) {
        try {
            ProcessExecution execution = executor.execute(getAAPTBadgingCommand(apk).getAsArray());
            return parseProperty(execution.getOutput(), "launchable-activity");
        } catch (AndroidExecutionException e) {
            logger.log(Level.SEVERE, "Execution exception while getting name of main application activity occured.", e);
            return null;
        }
    }

    /**
     *
     * @param apk apk file name to get the name of main activity from
     * @return name of main activity of application
     */
    public String getApplicationMainActivity(String apk) {
        return getApplicationMainActivity(new File(apk));
    }

    /**
     *
     * @param apk apk file to get the name of application base package of
     * @return name of application base package
     */
    public String getApplicationBasePackage(File apk) {

        try {
            ProcessExecution execution = executor.execute(getAAPTBadgingCommand(apk).getAsArray());
            return parseProperty(execution.getOutput(), "package");
        } catch (AndroidExecutionException e) {
            logger.log(Level.SEVERE, "Execution exception while getting name of main application package occured.", e);
            return null;
        }
    }

    /**
     *
     * @param apkFile file to get the list of activities of
     * @return list of activities which are in the package with fully qualified class name
     */
    public List<String> getActivities(File apkFile) {
        List<String> activities = new ArrayList<String>();

        try {
            ProcessExecution execution = executor.execute(getAAPTXmlTreeCommand(apkFile).getAsArray());
            activities = filterActivities(execution.getOutput());
        } catch (AndroidExecutionException e) {
            logger.log(Level.SEVERE, "Unable to get list of activities for file: " + apkFile.getAbsolutePath(), e);
        }
        return activities;
    }

    /**
     *
     * @param apkFile file to get the list of activities of
     * @return list of activities which are in the package with fully qualified class name
     */
    public List<String> getActivities(String apkFile) {
        return getActivities(new File(apkFile));
    }

    /**
     *
     * @param apk apk file name to get the name of application base package of
     * @return name of application base package
     */
    public String getApplicationBasePackage(String apk) {
        return getApplicationBasePackage(new File(apk));
    }

    /**
     * @param apk
     * @return command which dumps badging from {@code apk}
     */
    private Command getAAPTBadgingCommand(File apk) {
        Command command = new CommandBuilder().add(sdk.getAaptPath(), "dump", "badging", apk.getAbsolutePath()).build();
        logger.log(Level.FINE, command.toString());
        return command;
    }

    private Command getAAPTXmlTreeCommand(File apkFile) {
        Command command = new CommandBuilder()
            .add(sdk.getAaptPath(), "dump", "xmltree", apkFile.getAbsolutePath(), "AndroidManifest.xml")
            .build();
        logger.log(Level.FINE, command.toString());
        return command;
    }

    private List<String> filterActivities(List<String> output) {
        String packageName;
        List<String> activities = new ArrayList<String>();

        packageName = getPackageName(output);

        for (int i = 0; i < output.size(); i++) {
            if (output.get(i).trim().startsWith("E: activity")) {
                while (!output.get(++i).trim().contains("A: android:name")) {
                }

                String activityName = getActivityName(output.get(i));

                if (activityName.startsWith(".")) {
                    activities.add(packageName + activityName);
                } else if (!activityName.startsWith(".") && !activityName.contains(".")) {
                    activities.add(packageName + "." + activityName);
                } else {
                    activities.add(activityName);
                }
            }
        }

        return activities;
    }

    private String getPackageName(List<String> output) {
        for (String line : output) {
            if (line.contains("package")) {
                Pattern packagePattern = Pattern.compile("package=[\'\"]([^\'\"]+)[\'\"]");
                Matcher m = packagePattern.matcher(line);
                if (m.find()) {
                    return m.group(1);
                }
            }
        }
        throw new IllegalStateException("no package name found in dump xmltree for AndroidManifest.xml");
    }

    private String getActivityName(String line) {
        Pattern activityPattern = Pattern.compile("=[\"]([^\"]+)[\"]");
        Matcher m = activityPattern.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        throw new IllegalStateException("no activity name found in AndroidManifest.xml on line: " + line);
    }

    /**
     * Parses property value from list of lines, returns the first parsed result. Property to parse is somewhere at line in form
     * {@code property .... name=stringToGet}
     *
     * @param lines list of lines to get property value from
     * @param property property to get, is situated at the beginning of line
     * @return stringToGet
     */
    private String parseProperty(List<String> lines, String property) {
        if (lines == null || lines.size() == 0) {
            return null;
        }

        Pattern packagePattern = Pattern.compile("name=[\'\"]([^\'\"]+)[\'\"]");
        Matcher m;

        for (String line : lines) {
            if (line.contains(property)) {
                m = packagePattern.matcher(line);
                while (m.find()) {
                    return m.group(1);
                }
            }
        }
        return null;
    }

}