package com.databasepreservation.common.shared.client.common.visualization.browse.information;

import java.util.HashMap;
import java.util.Map;

import com.databasepreservation.common.shared.ViewerStructure.ViewerDatabase;
import com.databasepreservation.common.shared.ViewerStructure.ViewerMetadata;
import com.databasepreservation.common.shared.ViewerStructure.ViewerSchema;
import com.databasepreservation.common.shared.client.breadcrumb.BreadcrumbPanel;
import com.databasepreservation.common.shared.client.common.MetadataField;
import com.databasepreservation.common.shared.client.common.RightPanel;
import com.databasepreservation.common.shared.client.tools.BreadcrumbManager;
import com.databasepreservation.common.shared.client.tools.ViewerStringUtils;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimpleCheckBox;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

/**
 * @author Bruno Ferreira <bferreira@keep.pt>
 */
public class DatabaseInformationPanel extends RightPanel {
  private static final ClientMessages messages = GWT.create(ClientMessages.class);
  private static Map<String, DatabaseInformationPanel> instances = new HashMap<>();

  public static DatabaseInformationPanel getInstance(ViewerDatabase database) {
    String code = database.getUUID();

    DatabaseInformationPanel instance = instances.get(code);
    if (instance == null) {
      instance = new DatabaseInformationPanel(database);
      instances.put(code, instance);
    }
    return instance;
  }

  interface DatabaseInformationPanelUiBinder extends UiBinder<Widget, DatabaseInformationPanel> {
  }

  private static DatabaseInformationPanelUiBinder uiBinder = GWT.create(DatabaseInformationPanelUiBinder.class);

  private ViewerDatabase database;
  private boolean advancedMode = false; // True means advanced attributes are on, false means advanced view is off

  @UiField
  Label title;

  @UiField
  FlowPanel metadataContent, dataContent;


  @UiField
  SimpleCheckBox advancedSwitch;

  @UiField
  Label switchLabel, labelForSwitch;

  private DatabaseInformationPanel(ViewerDatabase database) {
    this.database = database;
    initWidget(uiBinder.createAndBindUi(this));

    init();
  }

  @Override
  public void handleBreadcrumb(BreadcrumbPanel breadcrumb) {
    BreadcrumbManager.updateBreadcrumb(breadcrumb, BreadcrumbManager.forDatabaseInformation(database.getUUID(), database.getMetadata().getName()));
  }

  private void init() {
    labelForSwitch.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        advancedSwitch.setValue(!advancedSwitch.getValue(), true); // workaround for ie11
        advancedMode = !advancedMode;
        metadataContent.clear();
        initMetadataContent();
        dataContent.clear();
        initDataContent();
      }
    });

    title.setText(messages.databaseInformationTextForTitle());
    switchLabel.setText(messages.schemaStructurePanelTextForAdvancedOption());
    initMetadataContent();
    initDataContent();
  }

  private void initDataContent() {
    if (database.getMetadata().getSchemas().size() == 1) {
      final DataPanel instance = DataPanel.getInstance(database, database.getMetadata().getSchemas().get(0).getUUID());
      instance.reload(advancedMode);
      dataContent.add(instance);
    } else {
      TabPanel tabPanel = new TabPanel();
      for (ViewerSchema schema : database.getMetadata().getSchemas()) {
        tabPanel.addStyleName("browseItemMetadata");
        final DataPanel instance = DataPanel.getInstance(database, schema.getUUID());
        instance.reload(advancedMode);
        tabPanel.add(instance, schema.getName());

      }
      tabPanel.selectTab(0);
      dataContent.add(tabPanel);
    }
  }

  private void initMetadataContent() {
    // database metadata
    ViewerMetadata metadata = database.getMetadata();

    metadataContent.add(getMetadataField(messages.siardMetadata_databaseName(), metadata.getName()));
    if (ViewerStringUtils.isNotBlank(metadata.getDescription())) {
      metadataContent.add(getMetadataField(messages.description(), metadata.getDescription()));
    } else {
      metadataContent.add(getMetadataField(messages.description(), messages.siardMetadata_DescriptionUnavailable()));
    }
    metadataContent
      .add(getMetadataField(messages.siardMetadata_dataOriginTimeSpan(), metadata.getDataOriginTimespan()));
    metadataContent.add(getMetadataField(messages.siardMetadata_dataOwner(), metadata.getDataOwner()));
    metadataContent.add(getMetadataField(messages.siardMetadata_archivalDate(),
      metadata.getArchivalDate() != null ? metadata.getArchivalDate().substring(0, 10) : metadata.getArchivalDate()));

    if (advancedMode) {
      metadataContent.add(getMetadataField(messages.siardMetadata_archivist(), metadata.getArchiver()));
      metadataContent.add(getMetadataField(messages.siardMetadata_archivistContact(), metadata.getArchiverContact()));
      metadataContent.add(getMetadataField(messages.siardMetadata_clientMachine(), metadata.getClientMachine()));
      metadataContent.add(getMetadataField(messages.siardMetadata_databaseProduct(), metadata.getDatabaseProduct()));
      metadataContent
        .add(getMetadataField(messages.siardMetadata_producerApplication(), metadata.getProducerApplication()));
    }
  }

  private MetadataField getMetadataField(String label, String value) {
    if (value != null) {
      MetadataField metadataField = MetadataField.createInstance(label, value);
      metadataField.setCSSMetadata("metadata-field", "metadata-information-element-label",
        "metadata-information-element-value");

      return metadataField;
    }

    final MetadataField instance = MetadataField.createInstance(label,
      messages.managePageTableHeaderTextForDatabaseStatus());
    instance.setCSSMetadata("metadata-field", "metadata-information-element-label",
      "metadata-information-element-value");

    return instance;
  }
}