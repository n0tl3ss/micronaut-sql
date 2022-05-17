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
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.service.ServiceRegistry;

import java.util.List;

/**
 * Internal factory.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Requires(missingBeans = LegacyInternalFactory.class)
@Factory
final class InternalFactory extends AbstractInternalFactory {

    InternalFactory(Environment environment,
                    List<SessionFactoryBuilderConfigure> configures,
                    StandardServiceRegistryBuilderCreator serviceRegistryBuilderSupplier,
                    List<StandardServiceRegistryBuilderConfigure> standardServiceRegistryBuilderConfigures) {
        super(environment, configures, serviceRegistryBuilderSupplier, standardServiceRegistryBuilderConfigures);
    }

    @EachBean(ServiceRegistry.class)
    MetadataSources buildMetadataSources(ServiceRegistry serviceRegistry) {
        return super.buildMetadataSources(serviceRegistry);
    }

    @EachBean(MetadataSources.class)
    Metadata buildMetadata(MetadataSources metadataSources, @Parameter JpaConfiguration jpaConfiguration) {
        return super.buildMetadata(metadataSources, jpaConfiguration);
    }

    @EachBean(JpaConfiguration.class)
    ServiceRegistry buildHibernateStandardServiceRegistry(JpaConfiguration jpaConfiguration) {
        return super.buildHibernateStandardServiceRegistry(jpaConfiguration);
    }

    @Context
    @Bean(preDestroy = "close")
    @EachBean(Metadata.class)
    SessionFactory buildHibernateSessionFactory(Metadata metadata, @Parameter JpaConfiguration jpaConfiguration) {
        return super.buildHibernateSessionFactory(metadata, jpaConfiguration);
    }

}
