package com.databasepreservation.common.client.models.structure;

import java.io.Serializable;

/**
 * @author Bruno Ferreira <bferreira@keep.pt>
 */
public class ViewerUserStructure implements Serializable {
  // mandatory in SIARD2
  private String name;

  // optional in SIARD2
  private String description;

  public ViewerUserStructure() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
