/* Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0 */

package com.aws.iot.evergreen.packagemanager;

import com.aws.iot.evergreen.packagemanager.exceptions.PackageVersionConflictException;
import com.aws.iot.evergreen.packagemanager.models.Package;
import com.aws.iot.evergreen.packagemanager.models.PackageMetadata;
import com.aws.iot.evergreen.packagemanager.models.PackageRegistryEntry;
import com.vdurmont.semver4j.Semver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PackageManager {


    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private PackageRegistry packageRegistry;

    private Set<PackageMetadata> proposedPackages;

    /*
     * Given a set of proposed package dependency trees.
     * Return the local resolved dependency tress in the future
     */
<<<<<<< HEAD
    public Future<Set<Package>> resolvePackages(Set<PackageMetadata> proposedPackages) {
=======
    public Future<Map<PackageMetadata, Package>> resolvePackages(Set<PackageMetadata> proposedPackages) {
        this.proposedPackages = proposedPackages;

        return executorService.submit((Callable<Map<PackageMetadata, Package>>) this::resolvePackages);
    }

    /*
     * Given a set of proposed package dependency trees,
     * figure out new package dependencies.
     */
    private Map<PackageMetadata, Package> resolvePackages() throws
            PackageVersionConflictException {
        Map<String, PackageRegistryEntry> activePackageList = packageRegistry.findActivePackages().stream()
                .collect(Collectors.toMap(PackageRegistryEntry::getName, Function.identity()));
        Set<PackageRegistryEntry> beforePackageSet = new HashSet<>(activePackageList.values());

        for (PackageMetadata proposedPackage : proposedPackages) {
            resolveDependencies(proposedPackage, activePackageList);
        }

        Set<PackageRegistryEntry> pendingDownloadPackages =
                activePackageList.values().stream().filter(p -> !beforePackageSet.contains(p))
                        .collect(Collectors.toSet());
        downloadPackages(pendingDownloadPackages);

        packageRegistry.updateActivePackages(new ArrayList<>(activePackageList.values()));

        return loadPackages(proposedPackages.stream().map(PackageMetadata::getName).collect(Collectors.toSet()));
    }

    void resolveDependencies(PackageMetadata packageMetadata, Map<String, PackageRegistryEntry> devicePackages)
            throws PackageVersionConflictException {

        Queue<PackageMetadata> processingQueue = new LinkedList<>();
        processingQueue.offer(packageMetadata);

        while (!processingQueue.isEmpty()) {
            PackageMetadata proposedPackage = processingQueue.poll();

            boolean useProposedPackage = true;
            // first to resolve package version
            // check if package exists on the device
            PackageRegistryEntry devicePackage = devicePackages.get(proposedPackage.getName());
            if (devicePackage != null) {
                // if exists, check if meets the proposed package constraint
                Semver devicePackageVersion = devicePackage.getVersion();
                if (devicePackageVersion != null && devicePackageVersion
                        .satisfies(proposedPackage.getVersionConstraint())) {
                    // device version meets the constraint, discard proposed version
                    useProposedPackage = false;
                } else {
                    // device version doesn't meet constraint, need to update
                    // check if proposed version meets existing package dependency constraint
                    for (PackageRegistryEntry.Reference dependsBy : devicePackage.getDependsBy().values()) {
                        if (!proposedPackage.getVersion().satisfies(dependsBy.getConstraint())) {
                            throw new PackageVersionConflictException("");
                        }
                    }
                }
            }

            // second to update its dependencies if necessary
            if (useProposedPackage) {
                devicePackage = new PackageRegistryEntry(proposedPackage.getName(), proposedPackage.getVersion(),
                        devicePackage == null ? new HashMap<>() : devicePackage.getDependsBy());
                devicePackages.put(proposedPackage.getName(), devicePackage);

                for (PackageMetadata proposedDependency : proposedPackage.getDependsOn()) {
                    devicePackage.getDependsOn().put(proposedDependency.getName(),
                            new PackageRegistryEntry.Reference(proposedDependency.getName(), null,
                                    proposedDependency.getVersionConstraint()));

                    PackageRegistryEntry dependencyPackageEntry = devicePackages.get(proposedDependency.getName());
                    if (dependencyPackageEntry == null) {
                        dependencyPackageEntry =
                                new PackageRegistryEntry(proposedDependency.getName(), null, new HashMap<>());
                        devicePackages.put(proposedDependency.getName(), dependencyPackageEntry);
                    }
                    PackageRegistryEntry.Reference dependBy =
                            dependencyPackageEntry.getDependsBy().get(proposedPackage.getName());
                    if (dependBy != null) {
                        dependBy.setVersion(proposedPackage.getVersion());
                        dependBy.setConstraint(proposedPackage.getVersionConstraint());
                    } else {
                        dependencyPackageEntry.getDependsBy().put(proposedPackage.getName(),
                                new PackageRegistryEntry.Reference(proposedPackage.getName(),
                                        proposedPackage.getVersion(), proposedDependency.getVersionConstraint()));
                    }

                    processingQueue.offer(proposedDependency);
                }
            }

            // third to update its dependent
            for (PackageRegistryEntry.Reference dependBy : devicePackage.getDependsBy().values()) {
                PackageRegistryEntry dependent = devicePackages.get(dependBy.getName());
                PackageRegistryEntry.Reference reference = dependent.getDependsOn().get(devicePackage.getName());
                if (reference.getVersion() == null || !reference.getVersion().isEqualTo(devicePackage.getVersion())) {
                    reference.setVersion(devicePackage.getVersion());
                }
            }
        }
    }

    /*
     * Given a set of pending refresh packages, download the package recipes and artifacts in background
     * Return the packages got successfully downloaded
     */
    private Set<PackageRegistryEntry> downloadPackages(Set<PackageRegistryEntry> pendingDownloadPackages) {
>>>>>>> package manager API definition
        return null;
    }

    /*
     * Given a set of target packages, return their resolved dependency trees and recipe data initialized
     * Given a set of target package names, return their resolved dependency trees with recipe data initialized
     */
    private Map<PackageMetadata, Package> loadPackages(Set<String> packageNames) {
        return Collections.emptyMap();
    }

}