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
package org.arquillian.droidium.container.automatic;

import java.io.File;

import org.arquillian.droidium.container.api.FileType;
import org.arquillian.droidium.container.api.IdentifierGenerator;
import org.arquillian.droidium.container.configuration.AndroidContainerConfiguration;
import org.arquillian.droidium.container.configuration.AndroidSDK;
import org.arquillian.droidium.container.sign.KeyStoreCreator;
import org.arquillian.droidium.container.utils.AndroidIdentifierGenerator;
import org.arquillian.spacelift.process.ProcessExecutor;
import org.arquillian.spacelift.process.impl.DefaultProcessExecutorFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests the creation of keystore which is needed for package signing.
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
@RunWith(JUnit4.class)
public class KeyStoreCreatorTestCase {

    private AndroidContainerConfiguration configuration;

    private AndroidSDK androidSDK;

    private KeyStoreCreator keyStoreCreator;

    private ProcessExecutor executor;

    private File keyStoreToCreate;

    @Before
    public void setup() {
        configuration = new AndroidContainerConfiguration();
        androidSDK = new AndroidSDK(configuration);
        executor = new DefaultProcessExecutorFactory().getProcessExecutorInstance();
        keyStoreCreator = new KeyStoreCreator(executor, androidSDK, configuration);

        IdentifierGenerator<FileType> aig = new AndroidIdentifierGenerator();
        keyStoreToCreate = new File(
            System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + aig.getIdentifier(FileType.FILE));
    }

    @After
    public void tearDown() {
        if (keyStoreToCreate.exists()) {
            boolean deleted = keyStoreToCreate.delete();
            if (!deleted) {
                throw new RuntimeException("Unable to delete files after test!");
            }
        }
    }

    @Test
    public void createKeyStoreTest() {
        keyStoreCreator.createKeyStore(keyStoreToCreate);
        Assert.assertTrue(keyStoreToCreate.exists());
    }
}