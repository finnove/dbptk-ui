package com.databasepreservation.main.common.client;

import java.util.HashMap;
import java.util.List;

import org.roda.core.data.exceptions.AuthenticationDeniedException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RODAException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.index.IndexResult;
import org.roda.core.data.v2.index.facet.Facets;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.sort.Sorter;
import org.roda.core.data.v2.index.sublist.Sublist;
import org.roda.core.data.v2.user.User;

import com.databasepreservation.main.common.shared.ViewerStructure.IsIndexed;
import com.databasepreservation.main.common.shared.ViewerStructure.ViewerDatabase;
import com.databasepreservation.main.common.shared.ViewerStructure.ViewerMetadata;
import com.databasepreservation.main.common.shared.ViewerStructure.ViewerRow;
import com.databasepreservation.main.common.shared.ViewerStructure.ViewerTable;
import com.databasepreservation.main.common.shared.client.common.search.SavedSearch;
import com.databasepreservation.main.common.shared.client.common.search.SearchField;
import com.databasepreservation.main.common.shared.client.common.search.SearchInfo;
import com.databasepreservation.main.desktop.shared.models.DBPTKModule;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * The client side stub for the browser service.
 */
@RemoteServiceRelativePath("browse")
public interface BrowserService extends RemoteService {
  /**
   * Service location
   */
  String SERVICE_URI = "browse";

  /**
   * Utilities
   */
  class Util {

    /**
     * @return the singleton instance
     */
    public static BrowserServiceAsync getInstance() {
      BrowserServiceAsync instance = GWT.create(BrowserService.class);
      ServiceDefTarget target = (ServiceDefTarget) instance;
      target.setServiceEntryPoint(GWT.getModuleBaseURL() + SERVICE_URI);
      return instance;
    }
  }

  List<SearchField> getSearchFields(ViewerTable viewerTable) throws GenericException;

  IndexResult<ViewerDatabase> findDatabases(Filter filter, Sorter sorter, Sublist sublist, Facets facets,
                                            String localeString) throws GenericException, AuthorizationDeniedException, RequestNotValidException;

  IndexResult<SavedSearch> findSavedSearches(String databaseUUID, Filter filter, Sorter sorter, Sublist sublist,
                                             Facets facets, String localeString)
    throws GenericException, AuthorizationDeniedException, RequestNotValidException, NotFoundException;

  <T extends IsIndexed> T retrieve(String databaseUUID, String classNameToReturn, String uuid)
    throws AuthorizationDeniedException, GenericException, NotFoundException;

  IndexResult<ViewerRow> findRows(String databaseUUID, Filter filter, Sorter sorter, Sublist sublist, Facets facets,
                                  String localeString) throws GenericException, AuthorizationDeniedException, RequestNotValidException;

  <T extends IsIndexed> Long countRows(String databaseUUID, Filter filter)
    throws AuthorizationDeniedException, GenericException, RequestNotValidException;

  <T extends IsIndexed> ViewerRow retrieveRows(String databaseUUID, String rowUUID)
    throws AuthorizationDeniedException, GenericException, NotFoundException;

  String getSolrQueryString(Filter filter, Sorter sorter, Sublist sublist, Facets facets)
    throws AuthorizationDeniedException, GenericException, RequestNotValidException;

  String saveSearch(String name, String description, String tableUUID, String tableName, String databaseUUID,
                    SearchInfo searchInfo)
    throws AuthorizationDeniedException, GenericException, RequestNotValidException, NotFoundException;

  void editSearch(String databaseUUID, String savedSearchUUID, String name, String description)
    throws AuthorizationDeniedException, GenericException, RequestNotValidException, NotFoundException;

  void deleteSearch(String databaseUUID, String savedSearchUUID)
    throws AuthorizationDeniedException, GenericException, RequestNotValidException, NotFoundException;

  Boolean isAuthenticationEnabled() throws RODAException;

  User getAuthenticatedUser() throws RODAException;

  User login(String username, String password) throws AuthenticationDeniedException, GenericException;

  String uploadSIARD(String path) throws GenericException;

  ViewerDatabase uploadSIARDStatus(String databaseUUID)
    throws GenericException, AuthorizationDeniedException, NotFoundException;

  String getReport(String databaseUUID) throws GenericException, AuthorizationDeniedException, NotFoundException;

  String getApplicationType();

  String uploadMetadataSIARD(String path) throws GenericException;

  String findSIARDFile(String path) throws GenericException, RequestNotValidException;

  DBPTKModule getDatabaseImportModules() throws GenericException;

  DBPTKModule getSIARDExportModule(String moduleName) throws GenericException;

  DBPTKModule getSIARDExportModules() throws GenericException;

  ViewerMetadata getSchemaInformation(String databaseUUID, String moduleName, HashMap<String, String> values) throws GenericException;

  boolean testConnection(String databaseUUID, String moduleName, HashMap<String, String> parameters) throws GenericException;
}