package com.databasepreservation.common.transformers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.databasepreservation.common.client.models.structure.ViewerColumn;
import org.apache.solr.common.SolrInputDocument;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import com.databasepreservation.common.client.index.filter.AndFiltersParameters;
import com.databasepreservation.common.client.index.filter.Filter;
import com.databasepreservation.common.client.index.filter.FilterParameter;
import com.databasepreservation.common.client.index.filter.SimpleFilterParameter;

import com.databasepreservation.common.client.ViewerConstants;
import com.databasepreservation.common.client.models.status.denormalization.DenormalizeConfiguration;
import com.databasepreservation.common.client.models.status.denormalization.ReferencesConfiguration;
import com.databasepreservation.common.client.models.status.denormalization.RelatedColumnConfiguration;
import com.databasepreservation.common.client.models.status.denormalization.RelatedTablesConfiguration;
import com.databasepreservation.common.client.models.structure.ViewerCell;
import com.databasepreservation.common.client.models.structure.ViewerDatabase;
import com.databasepreservation.common.client.models.structure.ViewerRow;
import com.databasepreservation.common.client.tools.FilterUtils;
import com.databasepreservation.common.server.ViewerConfiguration;
import com.databasepreservation.common.server.ViewerFactory;
import com.databasepreservation.common.server.index.DatabaseRowsSolrManager;
import com.databasepreservation.common.server.index.utils.IterableIndexResult;
import com.databasepreservation.common.server.index.utils.JsonTransformer;
import com.databasepreservation.model.exception.ModuleException;
import com.databasepreservation.utils.JodaUtils;

/**
 * @author Gabriel Barros <gbarros@keep.pt>
 */
public class DenormalizeTransformer {
  private final DatabaseRowsSolrManager solrManager;
  private final DenormalizeConfiguration denormalizeConfiguration;
  private final ViewerDatabase database;
  private final String databaseUUID;
  private final String jobUUID;
  private final String tableUUID;

  public DenormalizeTransformer(String databaseUUID, String tableUUID, String jobUUID) throws ModuleException {
    this.databaseUUID = databaseUUID;
    this.jobUUID = jobUUID;
    this.tableUUID = tableUUID;
    solrManager = ViewerFactory.getSolrManager();
    try {
      database = solrManager.retrieve(ViewerDatabase.class, this.databaseUUID);
      denormalizeConfiguration = getConfiguration(
        Paths.get(ViewerConstants.DENORMALIZATION_STATUS_PREFIX + tableUUID + ViewerConstants.JSON_EXTENSION),
        DenormalizeConfiguration.class);
      cleanNestedDocuments();
      queryOverRootTable();
      updateCollectionStatus();
    } catch (NotFoundException | GenericException e) {
      throw new ModuleException().withMessage("Cannot retrieved database from solr");
    }
  }

  private void cleanNestedDocuments() {
    Filter filter = FilterUtils.filterByTable(new Filter(), denormalizeConfiguration.getTableID());

    IterableIndexResult allRows = solrManager.findAllRows(databaseUUID, filter, null, new ArrayList<>());
    for (ViewerRow row : allRows) {
      solrManager.deleteNestedDocuments(databaseUUID, row.getUuid());
    }

    for (RelatedTablesConfiguration relatedTable : denormalizeConfiguration.getRelatedTables()) {
      solrManager.deleteNestedDocuments(databaseUUID, relatedTable.getUuid());
    }
  }

  private <T> T getConfiguration(Path path, Class<T> objectClass) throws ModuleException {
    Path configurationPath = ViewerConfiguration.getInstance().getDatabasesPath().resolve(database.getUuid())
      .resolve(path);
    if (Files.exists(configurationPath)) {
      return JsonTransformer.readObjectFromFile(configurationPath, objectClass);
    } else {
      throw new ModuleException().withMessage("Configuration file not exist: " + configurationPath.toString());
    }
  }

  private void updateCollectionStatus() throws GenericException {
    for (RelatedTablesConfiguration relatedTable : denormalizeConfiguration.getRelatedTables()) {
      setAllColumnsToInclude(relatedTable);
    }
  }

  private void setAllColumnsToInclude(RelatedTablesConfiguration relatedTable) throws GenericException {
    List<RelatedColumnConfiguration> columnsIncluded = relatedTable.getColumnsIncluded();

    if (!columnsIncluded.isEmpty()) {
      ViewerColumn viewerColumn = new ViewerColumn();
      viewerColumn.setDescription("Please EDIT");
      viewerColumn.setSolrName(relatedTable.getTableID());
      List<String> columnsId = new ArrayList<>();
      List<String> columnName = new ArrayList<>();

      for (RelatedColumnConfiguration column : columnsIncluded) {
        columnsId.add(column.getSolrName());
        columnName.add(column.getColumnName());
      }
      viewerColumn.setDisplayName(columnName.toString());
      ViewerFactory.getConfigurationManager().addDenormalizationColumns(databaseUUID,
        denormalizeConfiguration.getTableUUID(), viewerColumn, columnsId);
    }
    for (RelatedTablesConfiguration innerRelatedTable : relatedTable.getRelatedTables()) {
      setAllColumnsToInclude(innerRelatedTable);
    }

  }

  private void queryOverRootTable() throws ModuleException {
    String tableID = denormalizeConfiguration.getTableID();
    Filter filter = FilterUtils.filterByTable(new Filter(), tableID);

    List<RelatedTablesConfiguration> relatedTables = denormalizeConfiguration.getRelatedTables();
    List<String> fieldsToReturn = new ArrayList<>();
    fieldsToReturn.add(ViewerConstants.INDEX_ID);

    for (RelatedTablesConfiguration relatedTable : relatedTables) {
      for (ReferencesConfiguration reference : relatedTable.getReferences()) {
        fieldsToReturn.add(reference.getReferencedTable().getSolrName());
      }
    }

    // try {
    IterableIndexResult sourceRows = solrManager.findAllRows(databaseUUID, filter, null, fieldsToReturn);
    long processedRows = 0;
    long rowToProcess = sourceRows.getTotalCount();
    // solrManager.editBatchJob(jobUUID, rowToProcess, processedRows);
    for (ViewerRow row : sourceRows) {
      // solrManager.editBatchJob(jobUUID, rowToProcess, ++processedRows);
      List<SolrInputDocument> nestedDocuments = new ArrayList<>();
      for (RelatedTablesConfiguration relatedTable : denormalizeConfiguration.getRelatedTables()) {
        queryOverRelatedTables(row, relatedTable, nestedDocuments);
      }

      if (!nestedDocuments.isEmpty()) {
        solrManager.addDatabaseField(databaseUUID, row.getUuid(), nestedDocuments);
      }
    }
    // } catch (NotFoundException | GenericException e) {
    // throw new ModuleException().withMessage("Cannot update batch row in solr");
    // }
  }

  private void queryOverRelatedTables(ViewerRow row, RelatedTablesConfiguration relatedTable, List<SolrInputDocument> nestedDocuments) {
    String tableId = relatedTable.getTableID();
    Filter resultingFilter = new Filter();
    List<FilterParameter> filterParameterList = new ArrayList<>();
    List<String> fieldsToReturn = new ArrayList<>();

    fieldsToReturn.add(ViewerConstants.INDEX_ID);
    fieldsToReturn.add(ViewerConstants.SOLR_ROWS_TABLE_ID);

    filterParameterList.add(new SimpleFilterParameter(ViewerConstants.SOLR_ROWS_TABLE_ID, tableId));
    for (ReferencesConfiguration reference : relatedTable.getReferences()) {
      String sourceSolrName = reference.getSourceTable().getSolrName();
      String referencedSolrName = reference.getReferencedTable().getSolrName();
      String value = null;
      Map<String, ViewerCell> cells = row.getCells();
      for (Map.Entry<String, ViewerCell> entry : cells.entrySet()) {
        if (entry.getKey().equals(referencedSolrName)) {
          value = entry.getValue().getValue();
          break;
        }
      }
      if(value == null) return;
      filterParameterList.add(new SimpleFilterParameter(sourceSolrName, value));
    }

    resultingFilter.add(new AndFiltersParameters(filterParameterList));

    for (RelatedTablesConfiguration innerRelatedTable : relatedTable.getRelatedTables()) {
      for (ReferencesConfiguration reference : innerRelatedTable.getReferences()) {
        fieldsToReturn.add(reference.getReferencedTable().getSolrName());
      }
    }

    List<String> auxColumns = new ArrayList<>();
    for (ReferencesConfiguration reference : relatedTable.getReferences()) {
      auxColumns.add(reference.getReferencedTable().getSolrName());
    }

    List<String> columnsToDisplay = new ArrayList<>();
    for (RelatedColumnConfiguration relatedColumnConfiguration : relatedTable.getColumnsIncluded()) {
      columnsToDisplay.add(relatedColumnConfiguration.getSolrName());
    }

    fieldsToReturn.addAll(auxColumns);
    fieldsToReturn.addAll(columnsToDisplay);

    IterableIndexResult nestedRows = solrManager.findAllRows(databaseUUID, resultingFilter, null, fieldsToReturn);
    for (ViewerRow nestedRow : nestedRows) {
      for (RelatedTablesConfiguration innerRelatedTable : relatedTable.getRelatedTables()) {
        queryOverRelatedTables(nestedRow, innerRelatedTable, nestedDocuments);
      }
      if (!columnsToDisplay.isEmpty()) {
        createdNestedDocument(nestedRow, row.getUuid(), nestedDocuments);
      }
    }
  }

  private void createdNestedDocument(ViewerRow row, String parentUUID, List<SolrInputDocument> nestedDocuments) {
    Map<String, ViewerCell> cells = row.getCells();
    String uuid = parentUUID + ViewerConstants.API_SEP + row.getUuid();

    Map<String, Object> fields = new HashMap<>();
    for (Map.Entry<String, ViewerCell> cell : cells.entrySet()) {
      String key = cell.getKey();

      ViewerCell cellValue = cell.getValue();
      if (key.endsWith(ViewerConstants.SOLR_DYN_DATE)) {
        fields.put(key, JodaUtils.xsDateParse(cellValue.getValue()).toString());
      } else {
        fields.put(key, cellValue.getValue());
      }
    }
    if (!fields.isEmpty()) {
      nestedDocuments.add(solrManager.createNestedDocument(uuid, parentUUID, row.getUuid(), fields, row.getTableId(),
        row.getNestedUUID()));
    }
  }
}