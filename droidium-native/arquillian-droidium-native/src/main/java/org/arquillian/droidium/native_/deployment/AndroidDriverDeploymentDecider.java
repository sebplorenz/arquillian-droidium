/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
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
package org.arquillian.droidium.native_.deployment;

import java.io.File;

import org.arquillian.droidium.container.configuration.AndroidContainerConfiguration;
import org.arquillian.droidium.container.spi.event.AndroidDeploy;
import org.arquillian.droidium.native_.api.Instrumentable;
import org.arquillian.droidium.native_.configuration.DroidiumNativeConfiguration;
import org.arquillian.droidium.native_.instrumentation.DeploymentInstrumentationMapper;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.spi.event.container.AfterDeploy;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Decides if some deployment which is deployed not to Android device is going to be "instrumented". You can put
 * {@link Instrumentable} annotation to deployment which targets ordinary application container (deploying e.g. wars). When it
 * is "instrumented", it means that there will be Android driver apk from Selendroid installed on a device so you can test web
 * pages deployed to application containers from Android point of view.
 *
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public class AndroidDriverDeploymentDecider {

    @Inject
    private Instance<DeploymentInstrumentationMapper> deploymentInstrumentationMapper;

    @Inject
    private Instance<DroidiumNativeConfiguration> nativeConfiguration;

    @Inject
    private Event<AndroidDeploy> androidDeployEvent;

    public void decideAndroidServerDeployment(@Observes AfterDeploy event, DeploymentDescription description) {

        if (!event.getDeployableContainer().getConfigurationClass().equals(AndroidContainerConfiguration.class)) {
            if (deploymentInstrumentationMapper.get().getDeploymentName(description.getName()) != null) {
                androidDeployEvent.fire(new AndroidDeploy(resolveAndroidServerApkArchive()));
            }
        }

    }

    private Archive<?> resolveAndroidServerApkArchive() {
        File androidServerApk = nativeConfiguration.get().getAndroidDriverApk();
        return ShrinkWrap.createFromZipFile(JavaArchive.class, androidServerApk);
    }
}
