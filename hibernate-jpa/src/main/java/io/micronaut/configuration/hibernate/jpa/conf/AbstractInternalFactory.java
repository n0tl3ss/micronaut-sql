/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.hibernate.jpa.conf;

import io.micronaut.configuration.hibernate.jpa.JpaConfiguration;
import io.micronaut.configuration.hibernate.jpa.conf.serviceregistry.builder.configures.StandardServiceRegistryBuilderConfigure;
import io.micronaut.configuration.hibernate.jpa.conf.serviceregistry.builder.supplier.StandardServiceRegistryBuilderCreator;
import io.micronaut.configuration.hibernate.jpa.conf.sessionfactory.configure.SessionFactoryBuilderConfigure;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.env.Environment;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.StringUtils;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Abstract configuration factory.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
abstract class AbstractInternalFactory {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractInternalFactory.class);

    private final Environment environment;
    private final List<SessionFactoryBuilderConfigure> configures;
    private final StandardServiceRegistryBuilderCreator serviceRegistryBuilderSupplier;
    private final List<StandardServiceRegistryBuilderConfigure> standardServiceRegistryBuilderConfigures;

    AbstractInternalFactory(Environment environment,
                            List<SessionFactoryBuilderConfigure> configures,
                            StandardServiceRegistryBuilderCreator serviceRegistryBuilderSupplier,
                            List<StandardServiceRegistryBuilderConfigure> standardServiceRegistryBuilderConfigures) {
        this.environment = environment;
        this.configures = configures;
        this.serviceRegistryBuilderSupplier = serviceRegistryBuilderSupplier;
        this.standardServiceRegistryBuilderConfigures = standardServiceRegistryBuilderConfigures;
    }

    ServiceRegistry buildHibernateStandardServiceRegistry(JpaConfiguration jpaConfiguration) {
        StandardServiceRegistryBuilder standardServiceRegistryBuilder = serviceRegistryBuilderSupplier.create(jpaConfiguration);
        for (StandardServiceRegistryBuilderConfigure configure : standardServiceRegistryBuilderConfigures) {
            configure.configure(jpaConfiguration, standardServiceRegistryBuilder);
        }
        return standardServiceRegistryBuilder.build();
    }

    MetadataSources buildMetadataSources(ServiceRegistry serviceRegistry) {
        return new MetadataSources(serviceRegistry);
    }

    Metadata buildMetadata(MetadataSources metadataSources, JpaConfiguration jpaConfiguration) {
        jpaConfiguration.getEntityScanConfiguration().findEntities().forEach(metadataSources::addAnnotatedClass);

        if (jpaConfiguration.getMappingResources() != null) {
            for (String resource : jpaConfiguration.getMappingResources()) {
                metadataSources.addResource(resource);
            }
        }
        if (metadataSources.getAnnotatedClasses().isEmpty()) {
            String[] packages = jpaConfiguration.getEntityScanConfiguration().getPackages();
            if (ArrayUtils.isEmpty(packages)) {
                packages = environment.getPackages().toArray(StringUtils.EMPTY_STRING_ARRAY);
            }
            throw new ConfigurationException("Entities not found for JPA configuration: '" + jpaConfiguration.getName() + "' within packages [" + String.join(",", packages) + "]. Check that you have correctly specified a package containing JPA entities within the \"jpa." + jpaConfiguration.getName() + ".entity-scan.packages\" property in your application configuration and that those entities are either compiled with Micronaut or a build time index produced with @Introspected(packages=\"foo.bar\", includedAnnotations=Entity.class) declared on your Application class");
        }
        return metadataSources.buildMetadata();
    }

    SessionFactory buildHibernateSessionFactory(Metadata metadata, JpaConfiguration jpaConfiguration) {
        try {
            SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();
            for (SessionFactoryBuilderConfigure configure : configures) {
                configure.configure(jpaConfiguration, sessionFactoryBuilder);
            }
            return sessionFactoryBuilder.build();
        } catch (MappingException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Hibernate mapping error", e);
            }
            throw e;
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Error creating SessionFactory", e);
            }
            throw e;
        }
    }

}
