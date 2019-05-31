package com.databasepreservation.visualization.api;

import com.databasepreservation.visualization.api.v1.ExportsResource;
import com.databasepreservation.visualization.api.v1.LobsResource;
import com.databasepreservation.visualization.api.v1.ManageResource;
import com.databasepreservation.visualization.api.v1.ReportResource;
import com.databasepreservation.visualization.api.v1.ThemeResource;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestApplicationNoSwagger {

  @Configuration
  static class JerseyConfig extends ResourceConfig {
    /**
     * We just need the packages that have REST resources in them.
     */
    public JerseyConfig() {
      property(ServletProperties.FILTER_FORWARD_ON_404, true);
      property(ServletProperties.FILTER_CONTEXT_PATH,"/api/*");
      //packages("com.databasepreservation.visualization.api");
      register(ExportsResource.class);
      register(LobsResource.class);
      register(ManageResource.class);
      register(ReportResource.class);
      register(ThemeResource.class);
    }
  }
}