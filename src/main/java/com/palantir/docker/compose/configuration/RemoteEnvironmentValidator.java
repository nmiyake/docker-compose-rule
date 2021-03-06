/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.docker.compose.configuration;

import static com.google.common.collect.Sets.newHashSet;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RemoteEnvironmentValidator {

    private static final Set<String> SECURE_VARIABLES = ImmutableSet.of(DOCKER_TLS_VERIFY, DOCKER_CERT_PATH);

    private final Map<String, String> dockerEnvironment;

    public RemoteEnvironmentValidator(Map<String, String> dockerEnvironment) {
        this.dockerEnvironment = dockerEnvironment;
    }

    public Map<String, String> validate() {
        Collection<String> missingVariables = getMissingEnvVariables();
        String errorMessage = missingVariables.stream()
                                              .collect(joining(", ",
                                                               "Missing required environment variables: ",
                                                               ". Please run `docker-machine env <machine-name>` and "
                                                                       + "ensure they are set on the DockerComposition."));

        Preconditions.checkState(missingVariables.isEmpty(), errorMessage);
        return dockerEnvironment;
    }

    private Collection<String> getMissingEnvVariables() {
        Collection<String> requiredVariables = Sets.union(newHashSet(DOCKER_HOST), secureVariablesRequired());
        return requiredVariables.stream()
                                .filter(envVariable -> Strings.isNullOrEmpty(dockerEnvironment.get(envVariable)))
                                .collect(Collectors.toSet());
    }

    private Set<String> secureVariablesRequired() {
        return certVerificationEnabled() ? SECURE_VARIABLES : newHashSet();
    }

    private boolean certVerificationEnabled() {
        return dockerEnvironment.containsKey(DOCKER_TLS_VERIFY);
    }

    public static Map<String, String> validate(Map<String, String> dockerEnvironment) {
        return new RemoteEnvironmentValidator(dockerEnvironment).validate();
    }

}
