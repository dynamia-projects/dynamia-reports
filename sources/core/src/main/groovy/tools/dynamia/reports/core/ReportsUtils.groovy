package tools.dynamia.reports.core

import tools.dynamia.domain.ValidationError
import tools.dynamia.integration.Containers
import tools.dynamia.modules.saas.api.AccountServiceAPI
import tools.dynamia.reports.api.EntityFilterProvider
import tools.dynamia.reports.api.EnumFilterProvider
import tools.dynamia.reports.core.domain.Report
import tools.dynamia.reports.core.services.impl.ReportDataSource

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource
import java.sql.Connection

class ReportsUtils {

    static Connection getJdbcConnection(ReportDataSource dataSource) {
        if (dataSource.delegate instanceof Connection) {
            return dataSource.delegate as Connection
        } else if (dataSource.delegate instanceof DataSource) {
            return (dataSource.delegate as DataSource).connection
        } else {
            throw new ReportsException("Error obtaining database connection from report datasource $dataSource")
        }
    }

    static EntityManager getJpaEntityManager(ReportDataSource dataSource) {
        if (dataSource.delegate instanceof EntityManager) {
            return dataSource.delegate as EntityManager
        } else if (dataSource.delegate instanceof EntityManagerFactory) {
            return (dataSource.delegate as EntityManagerFactory).createEntityManager()
        } else {
            throw new ReportsException("Error getting entity manager from datasource " + dataSource)
        }
    }

    static EnumFilterProvider findEnumFilterProvider(String className) {
        return Containers.get().findObjects(EnumFilterProvider).find { it.enumClassName == className }
    }

    static EntityFilterProvider findEntityFilterProvider(String className) {
        return Containers.get().findObjects(EntityFilterProvider).find { it.entityClassName == className }
    }

    static List<EnumFilterProvider> findEnumFiltersProviders() {
        return Containers.get().findObjects(EnumFilterProvider)
    }

    static List<EntityFilterProvider> findEntityFiltersProvider() {
        return Containers.get().findObjects(EntityFilterProvider)
    }

    static String checkQuery(String query) {
        if (query == null) {
            throw new ValidationError("Invalid Query")
        }
        if (query.toLowerCase().contains("delete ") || query.toLowerCase().contains("update ")) {
            throw new ValidationError("Danger query detected: $query")
        }

        if (query.contains(":accountId")) {
            AccountServiceAPI accountServiceAPI = Containers.get().findObject(AccountServiceAPI)
            if (accountServiceAPI != null) {
                query = query.replaceAll(":accountId", String.valueOf(accountServiceAPI.getCurrentAccountId()))
            }
        }
        return query
    }

    static ReportDataSource findDatasource(Report report) {
        if (report.queryLang == "sql") {
            DataSource dataSource = Containers.get().findObject(DataSource.class)
            return new ReportDataSource("Database", dataSource)
        } else {
            EntityManagerFactory em = Containers.get().findObject(EntityManagerFactory.class)
            return new ReportDataSource("EntityManager", em)
        }

    }
}
