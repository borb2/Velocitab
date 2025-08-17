/*
 * This file is part of Velocitab, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.velocitab.config;

import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.desertwell.util.Version;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
@Configuration
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Metadata {

    private String velocityApiVersion;
    private int velocityMinimumBuild;
    private String papiProxyBridgeMinimumVersion;

    public void validateApiVersion(@NotNull Version version) {
        if (version.compareTo(Version.fromString(velocityApiVersion)) < 0) {
            final String serverVersion = version.toStringWithoutMetadata();
            throw new IllegalStateException("Your Velocity API version (" + serverVersion + ") is not supported! " +
                    "Disabling Velocitab. Please update to at least Velocity v" + velocityApiVersion
                    + " build #" + velocityMinimumBuild + " or newer.");
        }
    }

    public void validateBuild(@NotNull Version version) {
        try {
            int serverBuild = getBuildNumber(version.toString());
            if (serverBuild < velocityMinimumBuild) {
                throw new IllegalStateException("Your Velocity build version (#" + serverBuild + ") is not supported! " +
                        "Disabling Velocitab. Please update to at least Velocity v" + velocityApiVersion
                        + " build #" + velocityMinimumBuild + " or newer.");
            }
        } catch (IllegalArgumentException e) {
            // If we can't parse the build number, check if it's a development/snapshot version
            // and skip build validation for forks or custom builds
            if (isDevOrSnapshotVersion(version.toString())) {
                // Skip build validation for development/snapshot versions and forks
                return;
            }
            // Re-throw the exception if it's not a recognized development version
            throw e;
        }
    }

    public void validatePapiProxyBridgeVersion(@NotNull Version version) {
        if (version.compareTo(Version.fromString(papiProxyBridgeMinimumVersion)) < 0) {
            final String serverVersion = version.toStringWithoutMetadata();
            throw new IllegalStateException("Your PAPIProxyBridge version (" + serverVersion + ") is not supported! " +
                    "Disabling Velocitab. Please update to at least PAPIProxyBridge v" + papiProxyBridgeMinimumVersion  + ".");
        }
    }

    private int getBuildNumber(@NotNull String proxyVersion) {
        // Try multiple build number patterns
        Pattern[] patterns = {
            Pattern.compile(".*-b(\\d+).*"),           // Standard Velocity format: -b513
            Pattern.compile(".*build[\\s-](\\d+).*"),  // Alternative format: build 513, build-513
            Pattern.compile(".*#(\\d+).*"),            // Hash format: #513
            Pattern.compile(".*(\\d{3,}).*")           // Any 3+ digit number as fallback
        };
        
        for (Pattern pattern : patterns) {
            final Matcher matcher = pattern.matcher(proxyVersion);
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException ignored) {
                    // Continue to next pattern
                }
            }
        }
        
        throw new IllegalArgumentException("No build number found for proxy version: " + proxyVersion);
    }
    
    private boolean isDevOrSnapshotVersion(@NotNull String proxyVersion) {
        String lowerVersion = proxyVersion.toLowerCase();
        
        // Check for common development/snapshot indicators
        return lowerVersion.contains("snapshot") ||
               lowerVersion.contains("dev") ||
               lowerVersion.contains("git-") ||
               lowerVersion.contains("commit-") ||
               lowerVersion.contains("fork") ||
               lowerVersion.contains("custom") ||
               lowerVersion.contains("beta") ||
               lowerVersion.contains("alpha") ||
               lowerVersion.contains("rc") ||
               // VelocityCTD and other forks often have git hashes in parentheses
               lowerVersion.matches(".*\\(git-[a-f0-9]+\\).*");
    }

}
