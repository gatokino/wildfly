/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.web.infinispan.session;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.service.CacheBuilder;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationBuilder;
import org.wildfly.clustering.spi.CacheAliasBuilderProvider;
import org.wildfly.clustering.spi.CacheBuilderProvider;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * Creates routing services.
 * @author Paul Ferraro
 */
public class RouteCacheGroupBuilderProvider implements CacheBuilderProvider, CacheAliasBuilderProvider {

    static final String CACHE_NAME = "routing";

    private final Class<? extends CacheBuilderProvider> providerClass;

    RouteCacheGroupBuilderProvider(Class<? extends CacheBuilderProvider> providerClass) {
        this.providerClass = providerClass;
    }

    @Override
    public Collection<CapabilityServiceBuilder<?>> getBuilders(ServiceNameRegistry<ClusteringCacheRequirement> registry, String containerName, String aliasCacheName) {
        List<CapabilityServiceBuilder<?>> builders = new LinkedList<>();
        if (aliasCacheName == null) {
            builders.add(new TemplateConfigurationBuilder(ServiceName.parse(InfinispanCacheRequirement.CONFIGURATION.resolve(containerName, CACHE_NAME)), containerName, CACHE_NAME, aliasCacheName, builder -> {
                CacheMode mode = builder.clustering().cacheMode();
                builder.clustering().cacheMode(mode.isClustered() ? CacheMode.REPL_SYNC : CacheMode.LOCAL);
                builder.clustering().l1().disable();
                builder.persistence().clearStores();
            }));
            builders.add(new CacheBuilder<>(ServiceName.parse(InfinispanCacheRequirement.CACHE.resolve(containerName, CACHE_NAME)), containerName, CACHE_NAME));
            ServiceNameRegistry<ClusteringCacheRequirement> routingRegistry = requirement -> ServiceName.parse(requirement.resolve(containerName, CACHE_NAME));
            for (CacheBuilderProvider provider : ServiceLoader.load(this.providerClass, this.providerClass.getClassLoader())) {
                builders.addAll(provider.getBuilders(routingRegistry, containerName, CACHE_NAME));
            }
        }
        return builders;
    }

    @Override
    public Collection<CapabilityServiceBuilder<?>> getBuilders(ServiceNameRegistry<ClusteringCacheRequirement> registry, String containerName, String aliasCacheName, String targetCacheName) {
        return this.getBuilders(registry, containerName, aliasCacheName);
    }
}
