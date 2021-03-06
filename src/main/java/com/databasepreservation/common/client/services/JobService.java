package com.databasepreservation.common.client.services;

import java.util.function.Consumer;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.fusesource.restygwt.client.DirectRestService;
import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.REST;

import com.databasepreservation.common.client.ViewerConstants;
import com.databasepreservation.common.client.common.DefaultMethodCallback;
import com.databasepreservation.common.client.index.FindRequest;
import com.databasepreservation.common.client.index.IndexResult;
import com.databasepreservation.common.client.models.structure.ViewerJob;
import com.google.gwt.core.client.GWT;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * @author Gabriel Barros <gbarros@keep.pt>
 */
@Path(".." + ViewerConstants.ENDPOINT_JOB)
@Api(value = JobService.SWAGGER_ENDPOINT)
public interface JobService extends DirectRestService {
  String SWAGGER_ENDPOINT = "v1 job";

  class Util {
    /**
     * @return the singleton instance
     */
    public static JobService get() {
      return GWT.create(JobService.class);
    }

    public static <T> JobService call(MethodCallback<T> callback) {
      return REST.withCallback(callback).call(get());
    }

    public static <T> JobService call(Consumer<T> callback) {
      return REST.withCallback(DefaultMethodCallback.get(callback)).call(get());
    }

    public static <T> JobService call(Consumer<T> callback, Consumer<String> errorHandler) {
      return REST.withCallback(DefaultMethodCallback.get(callback, errorHandler)).call(get());
    }
  }

  @POST
  @Path("/find")
  @ApiOperation(value = "Find all jobs", response = IndexResult.class)
  IndexResult<ViewerJob> find(@ApiParam(ViewerConstants.API_QUERY_PARAM_FILTER) FindRequest findRequest,
    @QueryParam(ViewerConstants.API_QUERY_PARAM_LOCALE) String locale);
}
