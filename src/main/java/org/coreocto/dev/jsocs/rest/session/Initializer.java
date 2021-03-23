package org.coreocto.dev.jsocs.rest.session;

import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

public class Initializer extends AbstractHttpSessionApplicationInitializer {
	public Initializer() {
		super(HttpSessionConfig.class); 
	}
}
