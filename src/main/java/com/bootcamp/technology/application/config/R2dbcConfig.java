package com.bootcamp.technology.application.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * Configuración de transaccionalidad reactiva para R2DBC.
 *
 * <p>Registra un {@link ReactiveTransactionManager} respaldado por
 * {@link R2dbcTransactionManager} sobre el {@link ConnectionFactory} y un
 * {@link TransactionalOperator} programático. La transacción se propaga por el
 * {@code Context} de Reactor (no por {@code ThreadLocal}): commit al completar
 * el pipeline y rollback ante error, sin uso de {@code .block()}.
 */
@Configuration
public class R2dbcConfig {

    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager tm) {
        return TransactionalOperator.create(tm);
    }
}
