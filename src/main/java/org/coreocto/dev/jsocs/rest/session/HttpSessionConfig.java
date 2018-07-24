package org.coreocto.dev.jsocs.rest.session;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.transaction.PlatformTransactionManager;

@EnableJdbcHttpSession
public class HttpSessionConfig {
	
//	@Bean
//	public EmbeddedDatabase dataSource() {
//		org.springframework.jdbc.datasource.
//		return new EmbeddedDatabaseBuilder() // <2>
//				.setType(Rel)
//				.setType(EmbeddedDatabaseType.H2)
//				.addScript("org/springframework/session/jdbc/schema-h2.sql").build();
//	}

	@Bean
	public PlatformTransactionManager transactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource); // <3>
	}

}
