package com.databasepreservation.common.client.common.visualization.browse.configuration;

import java.util.HashMap;
import java.util.Map;

import com.databasepreservation.common.client.ViewerConstants;
import com.databasepreservation.common.client.models.structure.*;

/**
 * @author Gabriel Barros <gbarros@keep.pt>
 */
public class TableNode {
  private String uuid;
  private TableNode parentNode;
  private ViewerForeignKey foreignKey;
  private Map<ViewerForeignKey, TableNode> children;
  private ViewerDatabase database;
  private ViewerMetadata metadata;
  private ViewerTable table;

  public TableNode(ViewerDatabase database, ViewerTable table) {
    this.children = new HashMap<>();
    this.database = database;
    this.metadata = database.getMetadata();
    this.table = table;
  }

  public void setupChildren() {
    for (ViewerForeignKey foreignKey : table.getForeignKeys()) {
      ViewerTable viewerTable = metadata.getTable(foreignKey.getReferencedTableUUID());
      if (this.searchTop(viewerTable) == null) {
        TableNode childNode = new TableNode(database, viewerTable);
        childNode.uuid = generateUUID(foreignKey);
        children.put(foreignKey, childNode);
      }
    }

    for (ViewerSchema schema : metadata.getSchemas()) {
      for (ViewerTable viewerTable : schema.getTables()) {
        for (ViewerForeignKey foreignKey : viewerTable.getForeignKeys()) {
          if (foreignKey.getReferencedTableUUID().equals(table.getUuid()) && this.searchTop(viewerTable) == null) {
            TableNode childNode = new TableNode(database, viewerTable);
            childNode.uuid = generateUUID(foreignKey);
            children.put(foreignKey, childNode);
          }
        }
      }
    }
  }

  private String generateUUID(ViewerForeignKey foreignKey){
    StringBuilder uuid = new StringBuilder(table.getUuid());
    uuid.append(ViewerConstants.API_SEP).append(foreignKey.getReferencedTableUUID());
    for (ViewerReference reference : foreignKey.getReferences()) {
      uuid.append(ViewerConstants.API_SEP).append(reference.getSourceColumnIndex());
    }

    return uuid.toString();
  }

  public void setChildren(Map<ViewerForeignKey, TableNode> children) {
    this.children = children;
  }

  public TableNode searchTop(ViewerTable table) {
    if (this.getParentNode() == null) {
      return null;
    }
    if (this.getParentNode().table.equals(table)) {
      return this.getParentNode();
    }
    return this.getParentNode().searchTop(table);
  }

  public TableNode getParentNode() {
    return parentNode;
  }

  public void setParentNode(TableNode parentNode, ViewerForeignKey foreignKey) {
    this.parentNode = parentNode;
    this.foreignKey = foreignKey;
  }

  public ViewerTable getTable() {
    return table;
  }

  public Map<ViewerForeignKey, TableNode> getChildren() {
    return children;
  }

  public TableNode getChildrenByID(String tableUuid) {
    for (Map.Entry<ViewerForeignKey, TableNode> entry : children.entrySet()) {
      if (entry.getValue().table.getUuid().equals(tableUuid)) {
        return entry.getValue();
      }
    }
    return null;
  }

  public String getUuid() {
    return uuid;
  }

  public ViewerForeignKey getForeignKey() {
    return foreignKey;
  }
}